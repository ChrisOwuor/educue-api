package com.owuor.educue.results.entity;

import com.owuor.educue.results.enums.ResultStatus;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// NOTE: adjust this import to wherever your User entity actually lives.
import com.owuor.educue.users.entity.User;

@Entity
@Table(
        name = "student_results",
        indexes = {
                @Index(name = "idx_student_results_status", columnList = "status"),
                @Index(name = "idx_student_results_recorded_by", columnList = "recorded_by_id"),
                @Index(name = "idx_student_results_approved_by", columnList = "approved_by_id"),
        }
)
@Getter
@NoArgsConstructor
public class StudentResult {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid = UUID.randomUUID();

    @Version
    private Long version; // marks get contested and corrected — protect against concurrent edits

    // One result per attempt. A retake is a new StudentUnitRegistration row
    // (its own attemptType), so it gets its own result here rather than
    // overwriting the original attempt's marks. Same package, so this is
    // your actual entity, not a guess.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_unit_registration_id", nullable = false, unique = true)
    private StudentUnitRegistration studentUnitRegistration;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "ca_marks", precision = 5, scale = 2)
    private BigDecimal caMarks;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "exam_marks", precision = 5, scale = 2)
    private BigDecimal examMarks;

    // Derived from caMarks + examMarks — see recalculateTotal() below.
    // No public setTotalMarks(): it only changes as a side effect of
    // setting the two inputs it's made from, so it can't disagree with them.
    @Column(name = "total_marks", precision = 5, scale = 2)
    private BigDecimal totalMarks;

    // A plain string, not an enum: grade boundaries are institutional policy
    // that can differ by programme or change over time. Compute it in a
    // GradingService against a configurable scale — don't bake boundaries
    // into this entity.
    @Column(length = 10)
    private String grade;

    @Column(nullable = false)
    private boolean passed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status = ResultStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id")
    private User recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy; // null until status reaches APPROVED

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------
    // Controlled mutation. No blanket @Setter — id, uuid, version,
    // createdAt and totalMarks should never be set directly from outside.
    // -------------------------------------------------------------------

    public void setCaMarks(BigDecimal caMarks) {
        this.caMarks = caMarks;
        recalculateTotal();
    }

    public void setExamMarks(BigDecimal examMarks) {
        this.examMarks = examMarks;
        recalculateTotal();
    }
    public void setStudentUnitRegistration(
            StudentUnitRegistration registration
    ) {
        this.studentUnitRegistration = registration;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public void submit() {
        this.status = ResultStatus.SUBMITTED;
    }

    public void approve(User approver) {

        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
        this.status = ResultStatus.APPROVED;
    }

    public void release() {
        if (status != ResultStatus.APPROVED) {
            throw new IllegalStateException(
                    "Cannot release a result that hasn't been approved");
        }
        this.status = ResultStatus.RELEASED;
    }

    public void withhold(String reason) {
        this.status = ResultStatus.WITHHELD;
    }

    // Called by GradingService once it has computed grade/passed from
    // totalMarks against the applicable scale.
    public void applyGrade(String grade, boolean passed) {
        this.grade = grade;
        this.passed = passed;
    }

    private void recalculateTotal() {
        this.totalMarks = (caMarks != null && examMarks != null)
                ? caMarks.add(examMarks)
                : null;
    }

    // Defense in depth: guarantees totalMarks is correct at save time
    // regardless of how the fields were set (e.g. a mapper library writing
    // fields directly rather than going through the setters above).
    @PrePersist
    @PreUpdate
    private void onSave() {
        recalculateTotal();
    }
}
