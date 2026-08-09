package com.owuor.educue.finance.entity;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.admissions.entity.Intake;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "graduation_fee_items", uniqueConstraints = @UniqueConstraint(
        name = "uq_graduation_fee_item_start",
        columnNames = {"qualification_type", "fee_item_id", "effective_from_intake_sequence"}
), indexes = @Index(name = "idx_graduation_fee_effective", columnList =
        "qualification_type,effective_from_intake_sequence,effective_to_intake_sequence"))
@Check(constraints = "amount >= 0 AND (effective_to_intake_sequence IS NULL OR effective_to_intake_sequence > effective_from_intake_sequence)")
@Getter
@Setter
@NoArgsConstructor
public class GraduationFeeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification_type", nullable = false, length = 30, updatable = false)
    private QualificationType qualificationType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_item_id", nullable = false, updatable = false)
    private FeeItem feeItem;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "effective_from_intake_sequence", referencedColumnName = "sequence_number", nullable = false, updatable = false)
    private Intake effectiveFromIntake;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effective_to_intake_sequence", referencedColumnName = "sequence_number")
    private Intake effectiveToIntake;

    @Column(nullable = false)
    private boolean mandatory = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        validateRange();
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void update() {
        validateRange();
        updatedAt = LocalDateTime.now();
    }

    private void validateRange() {
        if (amount == null || amount.signum() < 0) throw new IllegalStateException("Graduation fee amount must be zero or greater");
        if (qualificationType == null || feeItem == null || effectiveFromIntake == null) throw new IllegalStateException("Graduation fee scope is incomplete");
        if (effectiveToIntake != null && effectiveToIntake.getSequenceNumber() <= effectiveFromIntake.getSequenceNumber()) {
            throw new IllegalStateException("Effective-to intake must come after effective-from intake");
        }
    }
}
