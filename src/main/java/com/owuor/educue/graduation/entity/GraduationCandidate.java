package com.owuor.educue.graduation.entity;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.graduation.enums.AwardClassification;
import com.owuor.educue.graduation.enums.GraduationCandidateStatus;
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

@Entity
@Table(
        name = "graduation_candidates",
        uniqueConstraints = @UniqueConstraint(name = "uq_graduation_entry_list_enrollment", columnNames = {"graduation_list_id", "enrollment_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class GraduationCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * The enrollment represented by this graduation-list candidate.
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "graduation_list_id", nullable = false)
    private GraduationList graduationList;

    @Column(name = "admission_number_snapshot", length = 80)
    private String admissionNumberSnapshot;
    @Column(name = "course_code_snapshot", length = 50)
    private String courseCodeSnapshot;
    @Column(name = "course_name_snapshot", length = 200)
    private String courseNameSnapshot;
    @Column(name = "graduation_name", length = 180)
    private String graduationName;
    @Column(name = "hod_remarks", length = 1000)
    private String hodRemarks;
    @Column(name = "details_confirmed_at")
    private LocalDateTime detailsConfirmedAt;
    @Column(name = "finance_cleared")
    private Boolean financeCleared = false;

    /** Candidate workflow state. NOT_STARTED means added to a draft list. */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    private GraduationCandidateStatus status = GraduationCandidateStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "clearance_stage", nullable = false, length = 30)
    private GraduationClearanceStage clearanceStage = GraduationClearanceStage.NOT_STARTED;

    /*
     * Official award snapshot.
     *
     * Example:
     * Bachelor of Science in Nursing
     *
     * It is copied from Course when the HOD adds the candidate
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
     * Stored academic-readiness snapshot retained for registrar review.
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

    /** Audit guard that prevents accidental duplicate certificate printing. */
    @Column(name = "certificate_printed", nullable = false)
    private boolean certificatePrinted;

    @Column(name = "certificate_printed_at")
    private LocalDateTime certificatePrintedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_printed_by")
    private User certificatePrintedBy;

    @Column(name = "certificate_number", length = 60, unique = true)
    private String certificateNumber;

    @Column(
            name = "added_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime addedAt;

    /** Temporary academic assessment. It may be deleted after graduation. */
    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private GraduationReadiness readiness;

    @Column(name = "clearance_complete")
    private Boolean clearanceComplete;

    @Version
    private Long version;

    @PrePersist
    public void onCreate() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }

        if (status == null) {
            status = GraduationCandidateStatus.NOT_STARTED;
        }
    }
}
