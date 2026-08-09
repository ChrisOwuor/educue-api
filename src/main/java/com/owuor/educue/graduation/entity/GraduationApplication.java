package com.owuor.educue.graduation.entity;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.finance.entity.FeeLedger;
import com.owuor.educue.graduation.enums.AwardClassification;
import com.owuor.educue.graduation.enums.GraduationApplicationStatus;
import com.owuor.educue.graduation.enums.GraduationClearanceStage;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "graduation_applications",
        uniqueConstraints = @UniqueConstraint(name="uq_graduation_entry_list_enrollment",columnNames={"graduation_list_id","enrollment_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class GraduationApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * One graduation application per student enrollment.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "enrollment_id",
            nullable = false,
            updatable = false
    )
    private Enrollment enrollment;

    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="graduation_list_id",nullable=false)
    private GraduationList graduationList;

    @Column(name="admission_number_snapshot",length=80) private String admissionNumberSnapshot;
    @Column(name="course_code_snapshot",length=50) private String courseCodeSnapshot;
    @Column(name="course_name_snapshot",length=200) private String courseNameSnapshot;
    @Column(name="graduation_name",length=180) private String graduationName;
    @Column(name="hod_remarks",length=1000) private String hodRemarks;
    @Column(name="details_confirmed_at") private LocalDateTime detailsConfirmedAt;
    @Column(name="finance_cleared") private Boolean financeCleared=false;

    /*
     * APPLIED when submitted.
     *
     * Final cumulative average and classification are
     * populated later when the registrar approves it.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    private GraduationApplicationStatus status =
            GraduationApplicationStatus.APPLIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "clearance_stage", nullable = false, length = 30)
    private GraduationClearanceStage clearanceStage = GraduationClearanceStage.NOT_STARTED;

    /*
     * Official award snapshot.
     *
     * Example:
     * Bachelor of Science in Nursing
     *
     * It is copied from Course when the student applies
     * and cannot be changed through normal JPA updates.
     */
    @Column(
            name = "award_title",
            nullable = false,
            updatable = false,
            length = 200
    )
    private String awardTitle;

    /*
     * Qualification category snapshot.
     *
     * Example:
     * DEGREE, DIPLOMA, CERTIFICATE
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "qualification_type",
            nullable = false,
            updatable = false,
            length = 50
    )
    private QualificationType qualificationType;

    /*
     * These are intentionally nullable at application time.
     *
     * They are populated only during academic approval.
     */
    @Column(
            name = "final_cumulative_average",
            precision = 5,
            scale = 2
    )
    private BigDecimal finalCumulativeAverage;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "award_classification",
            length = 80
    )
    private AwardClassification awardClassification;

    /*
     * The registrar/admin who finalized the academic result.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_approval_by")
    private User academicApprovalBy;

    @Column(name = "academic_approved_at")
    private LocalDateTime academicApprovedAt;

    /*
     * Graduation charge.
     */
    @Column(
            name = "total_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalAmount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ledger_entry_id",
            unique = true
    )
    private FeeLedger ledgerEntry;

    /*
     * Snapshot of the graduation fee configuration that
     * applied when the student submitted the application.
     */
    @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC, id ASC")
    private List<GraduationApplicationFeeItem> feeItems =
            new ArrayList<>();

    @Column(
            name = "applied_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime appliedAt;

    /*
     * Graduation-readiness snapshot.
     */
    @Column(name = "required_units")
    private Integer requiredUnits;

    @Column(name = "passed_units")
    private Integer passedUnits;

    @Column(name = "failed_units")
    private Integer failedUnits;

    @Column(name = "missing_results")
    private Integer missingResults;

    @Column(name = "missing_units")
    private Integer missingUnits;

    @Column(name = "required_credits")
    private Integer requiredCredits;

    @Column(name = "earned_credits")
    private Integer earnedCredits;

    @Column(name = "clearance_complete")
    private Boolean clearanceComplete;

    @Column(name = "eligibility_assessed_at")
    private LocalDateTime eligibilityAssessedAt;

    /*
     * Graduation event/batch information.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graduation_batch_id")
    private GraduationBatch graduationBatch;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    /*
     * Conferring is different from academic approval.
     *
     * academicApprovalBy confirms average/classification.
     * conferredBy confirms the award was formally conferred.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conferred_by")
    private User conferredBy;

    @Column(name = "conferred_at")
    private LocalDateTime conferredAt;

    @Version
    private Long version;

    @PrePersist
    public void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }

        if (status == null) {
            status = GraduationApplicationStatus.APPLIED;
        }
    }

    public void addFeeItem(
            GraduationApplicationFeeItem item
    ) {
        item.setApplication(this);
        feeItems.add(item);
    }
}
