package com.owuor.educue.finance.entity;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.admissions.entity.Intake;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "period_fee_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_period_fee_item_start",
                        columnNames = {
                                "course_academic_period_id",
                                "fee_item_id",
                                "effective_from_intake_sequence"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_period_fee_items_course_period",
                        columnList = "course_academic_period_id"
                ),
                @Index(
                        name = "idx_period_fee_items_period_fee",
                        columnList = "course_academic_period_id, fee_item_id"
                ),
                @Index(
                        name = "idx_period_fee_items_effective_range",
                        columnList = """
                course_academic_period_id,
                effective_from_intake_sequence,
                effective_to_intake_sequence
                """
                )
        }
)
@Check(
        constraints = """
        amount >= 0
        AND (
            effective_to_intake_sequence IS NULL
            OR effective_to_intake_sequence
                > effective_from_intake_sequence
        )
        """
)
@Getter
@Setter
@NoArgsConstructor
public class PeriodFeeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The course period whose fees are being configured.
     *
     * Examples:
     * BIT Y1S1
     * BIT Y1S2
     * Nursing Y1T1
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_academic_period_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_period_fee_item_course_period"
            )
    )
    private CourseAcademicPeriod courseAcademicPeriod;

    /**
     * Global reusable fee definition.
     *
     * Examples:
     * Tuition
     * Medical
     * Registration
     * Technology
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "fee_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_period_fee_item_fee_item"
            )
    )
    private FeeItem feeItem;

    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    /**
     * Inclusive starting intake.
     *
     * This FK references intakes.sequence_number,
     * not intakes.id.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "effective_from_intake_sequence",
            referencedColumnName = "sequence_number",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                    name = "fk_period_fee_item_from_intake"
            )
    )
    private Intake effectiveFromIntake;

    /**
     * Exclusive ending intake.
     *
     * NULL means the fee continues to all future intakes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "effective_to_intake_sequence",
            referencedColumnName = "sequence_number",
            foreignKey = @ForeignKey(
                    name = "fk_period_fee_item_to_intake"
            )
    )
    private Intake effectiveToIntake;

    @Column(nullable = false)
    private boolean mandatory = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        validate();

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        validate();
        updatedAt = LocalDateTime.now();
    }

    private void validate() {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalStateException(
                    "Fee amount must be zero or greater"
            );
        }

        if (courseAcademicPeriod == null) {
            throw new IllegalStateException(
                    "Course academic period is required"
            );
        }

        if (feeItem == null) {
            throw new IllegalStateException(
                    "Fee item is required"
            );
        }

        if (effectiveFromIntake == null) {
            throw new IllegalStateException(
                    "Effective-from intake is required"
            );
        }

        if (effectiveToIntake != null) {
            Long fromSequence =
                    effectiveFromIntake.getSequenceNumber();

            Long toSequence =
                    effectiveToIntake.getSequenceNumber();

            if (
                    fromSequence != null
                    && toSequence != null
                    && toSequence <= fromSequence
            ) {
                throw new IllegalStateException(
                        "Effective-to intake must come after "
                        + "effective-from intake"
                );
            }
        }
    }
}
