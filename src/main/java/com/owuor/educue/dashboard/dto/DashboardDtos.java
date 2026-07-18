package com.owuor.educue.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record TimePoint(String label, BigDecimal primary, BigDecimal secondary) {}
    public record Breakdown(String label, BigDecimal value) {}
    public record SeriesPoint(String label, BigDecimal value) {}
    public record CourseSeries(String course, List<SeriesPoint> points) {}
    public record Activity(String type, String title, String detail, LocalDateTime occurredAt) {}

    public record FinanceDashboard(
            BigDecimal collectedThisMonth, BigDecimal collectedThisYear,
            BigDecimal totalCharges, BigDecimal outstandingBalance,
            long successfulPayments, long pendingPayments,
            List<TimePoint> monthlyCashFlow, List<Breakdown> paymentMethods,
            List<CourseSeries> collectionByCourse, List<Activity> recentPayments) {}

    public record AdminDashboard(
            long totalStudents, long activeCourses, long pendingApplications,
            BigDecimal collectedThisMonth, long activeEnrollments,
            List<TimePoint> monthlyGrowth, List<Breakdown> applicationStatuses,
            List<CourseSeries> studentsByCourse, List<Activity> recentActivity) {}
}
