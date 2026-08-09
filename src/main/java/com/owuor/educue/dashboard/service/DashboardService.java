package com.owuor.educue.dashboard.service;

import com.owuor.educue.dashboard.dto.DashboardDtos.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final EntityManager entityManager;

    public FinanceDashboard finance() { return finance(null, null, 12); }

    public FinanceDashboard finance(Integer requestedFromYear, Integer requestedToYear, Integer requestedMonths) {
        int currentYear = Year.now().getValue();
        int fromYear = requestedFromYear == null ? currentYear - 3 : requestedFromYear;
        int toYear = requestedToYear == null ? currentYear + 3 : requestedToYear;
        if (fromYear > toYear || toYear - fromYear > 20) {
            throw new IllegalArgumentException("The collection year range must be ordered and cannot exceed 21 years");
        }
        int months = requestedMonths == null ? 12 : requestedMonths;
        if (months < 1 || months > 36) {
            throw new IllegalArgumentException("The monthly collection range must be between 1 and 36 months");
        }
        BigDecimal month = money("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='VERIFIED' AND paid_at >= date_trunc('month', CURRENT_DATE)");
        BigDecimal year = money("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='VERIFIED' AND paid_at >= date_trunc('year', CURRENT_DATE)");
        BigDecimal charges = money("SELECT COALESCE(SUM(debit),0) FROM fee_ledger");
        BigDecimal outstanding = money("SELECT COALESCE(SUM(debit - credit), 0) FROM fee_ledger");
        long completed = count("SELECT COUNT(*) FROM payments WHERE status='VERIFIED'");
        long pending = count("SELECT COUNT(*) FROM payments WHERE status='PENDING'");
        BigDecimal settled = charges.subtract(outstanding).max(BigDecimal.ZERO);
        return new FinanceDashboard(month, year, charges, outstanding, completed, pending,
                timeSeries("""
                    WITH months AS (SELECT generate_series(date_trunc('month', CURRENT_DATE) - interval '11 months', date_trunc('month', CURRENT_DATE), interval '1 month') AS period_start)
                    SELECT to_char(m.period_start,'Mon'), COALESCE(SUM(p.amount) FILTER (WHERE p.status='VERIFIED'),0),
                           COALESCE((SELECT SUM(fl.debit) FROM fee_ledger fl WHERE date_trunc('month',fl.created_at)=m.period_start),0)
                    FROM months m LEFT JOIN payments p ON date_trunc('month',p.paid_at)=m.period_start GROUP BY m.period_start ORDER BY m.period_start
                    """), breakdown("SELECT payment_method, SUM(amount) FROM payments WHERE status='VERIFIED' GROUP BY payment_method ORDER BY SUM(amount) DESC"),
                List.of(new Breakdown("Settled", settled), new Breakdown("Outstanding", outstanding.max(BigDecimal.ZERO))),
                breakdown("SELECT status, COUNT(*) FROM payments GROUP BY status ORDER BY status"),
                monthlyCollections(months), yearlyCollections(fromYear, toYear), activities("""
                    SELECT 'PAYMENT', s.full_name, concat(COALESCE(p.receipt_number,p.gateway_reference),' · ',p.payment_method,' · KES ',to_char(p.amount,'FM999,999,990.00')),p.paid_at
                    FROM payments p JOIN students s ON s.id=p.student_id ORDER BY p.paid_at DESC LIMIT 8
                    """));
    }

    public AdminDashboard admin() {
        return new AdminDashboard(count("SELECT COUNT(*) FROM students"), count("SELECT COUNT(*) FROM courses WHERE active=true"),
                count("SELECT COUNT(*) FROM applications WHERE status='PENDING'"),
                money("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='VERIFIED' AND paid_at >= date_trunc('month',CURRENT_DATE)"),
                count("SELECT COUNT(*) FROM enrollments WHERE status='ACTIVE'"),
                timeSeries("""
                    WITH months AS (SELECT generate_series(date_trunc('month',CURRENT_DATE)-interval '11 months',date_trunc('month',CURRENT_DATE),interval '1 month') AS period_start)
                    SELECT to_char(m.period_start,'Mon'),
                           (SELECT COUNT(*) FROM students s WHERE date_trunc('month',s.created_at)=m.period_start),
                           COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.status='VERIFIED' AND date_trunc('month',p.paid_at)=m.period_start),0)
                    FROM months m ORDER BY m.period_start
                    """), breakdown("SELECT status,COUNT(*) FROM applications GROUP BY status ORDER BY COUNT(*) DESC"),
                courseSeries("""
                    WITH months AS (SELECT generate_series(date_trunc('month',CURRENT_DATE)-interval '11 months',date_trunc('month',CURRENT_DATE),interval '1 month') AS period_start),
                    course_set AS (SELECT c.id,c.name FROM courses c)
                    SELECT cs.name,to_char(m.period_start,'Mon'),COUNT(e.id)
                    FROM course_set cs CROSS JOIN months m
                    LEFT JOIN enrollments e ON e.course_id=cs.id AND date_trunc('month',e.created_at)=m.period_start
                    GROUP BY cs.name,m.period_start ORDER BY cs.name,m.period_start
                    """), activities("""
                    (SELECT 'APPLICATION',full_name,concat(application_number,' · ',status),submitted_at FROM applications)
                    UNION ALL
                    (SELECT 'PAYMENT',s.full_name,concat(COALESCE(p.receipt_number,p.gateway_reference),' · KES ',to_char(p.amount,'FM999,999,990.00')),p.paid_at FROM payments p JOIN students s ON s.id=p.student_id)
                    ORDER BY 4 DESC LIMIT 10
                    """));
    }

    private BigDecimal money(String sql) { return new BigDecimal(entityManager.createNativeQuery(sql).getSingleResult().toString()); }
    private long count(String sql) { return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue(); }
    private List<TimePoint> timeSeries(String sql) { return rows(sql).stream().map(r -> new TimePoint(r[0].toString(), decimal(r[1]), decimal(r[2]))).toList(); }
    private List<Breakdown> breakdown(String sql) { return rows(sql).stream().map(r -> new Breakdown(r[0].toString(), decimal(r[1]))).toList(); }
    private List<Breakdown> yearlyCollections(int fromYear, int toYear) {
        return breakdown("""
                WITH years AS (SELECT generate_series(:fromYear, :toYear) AS year)
                SELECT y.year::text, COALESCE(SUM(p.amount), 0)
                FROM years y LEFT JOIN payments p ON p.status='VERIFIED' AND EXTRACT(YEAR FROM p.paid_at)=y.year
                GROUP BY y.year ORDER BY y.year
                """, fromYear, toYear);
    }
    private List<Breakdown> monthlyCollections(int months) {
        String sql = """
                WITH months AS (SELECT generate_series(date_trunc('month', CURRENT_DATE) - make_interval(months => :offset), date_trunc('month', CURRENT_DATE), interval '1 month') AS period_start)
                SELECT to_char(m.period_start, 'Mon YY'), COALESCE(SUM(p.amount), 0)
                FROM months m LEFT JOIN payments p ON p.status='VERIFIED' AND date_trunc('month', p.paid_at)=m.period_start
                GROUP BY m.period_start ORDER BY m.period_start
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> result = entityManager.createNativeQuery(sql).setParameter("offset", months - 1).getResultList();
        return result.stream().map(r -> new Breakdown(r[0].toString(), decimal(r[1]))).toList();
    }
    @SuppressWarnings("unchecked")
    private List<Breakdown> breakdown(String sql, int fromYear, int toYear) {
        return ((List<Object[]>) entityManager.createNativeQuery(sql)
                .setParameter("fromYear", fromYear).setParameter("toYear", toYear).getResultList())
                .stream().map(r -> new Breakdown(r[0].toString(), decimal(r[1]))).toList();
    }
    private List<CourseSeries> courseSeries(String sql) {
        var grouped = new java.util.LinkedHashMap<String, java.util.List<SeriesPoint>>();
        for (Object[] row : rows(sql)) grouped.computeIfAbsent(row[0].toString(), ignored -> new java.util.ArrayList<>())
                .add(new SeriesPoint(row[1].toString(), decimal(row[2])));
        return grouped.entrySet().stream().map(entry -> new CourseSeries(entry.getKey(), entry.getValue())).toList();
    }
    private List<Activity> activities(String sql) { return rows(sql).stream().map(r -> new Activity(r[0].toString(), r[1].toString(), r[2].toString(), date(r[3]))).toList(); }
    @SuppressWarnings("unchecked") private List<Object[]> rows(String sql) { return entityManager.createNativeQuery(sql).getResultList(); }
    private BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()); }
    private LocalDateTime date(Object value) { return value instanceof Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) value; }
}
