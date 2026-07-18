package com.owuor.educue.finance.entity;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.finance.enums.TransactionType;
import com.owuor.educue.students.entity.Student;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.owuor.educue.finance.enums.LedgerStatus;

@Entity
@Table(
        name = "fee_ledger",
        indexes = {
                @Index(name = "idx_fee_ledger_student", columnList = "student_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FeeLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_academic_period_id")
    private CourseAcademicPeriod courseAcademicPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id")
    private FeeStructure feeStructure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Column(name = "document_number", nullable = false, unique = true, updatable = false, length = 40)
    private String documentNumber;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerStatus status = LedgerStatus.POSTED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id", unique = true)
    private FeeLedger reversalOf;

    @Column(nullable = false, length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
