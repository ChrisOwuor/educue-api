package com.owuor.educue.academics.entity;

import com.owuor.educue.academics.enums.AcademicPeriodType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "academic_periods",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_academic_period_code",
                        columnNames = {"code"}
                ),
                @UniqueConstraint(
                        name = "uk_academic_period_type_year_number",
                        columnNames = {
                                "period_type",
                                "year_number",
                                "period_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_academic_period_type",
                        columnList = "period_type"
                ),
                @Index(
                        name = "idx_academic_period_sequence",
                        columnList = "period_type, sequence_number"
                ),
                @Index(
                        name = "idx_academic_period_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Public-safe identifier used by the API.
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    /*
     * Short identifier displayed throughout the system.
     *
     * Examples:
     * Y1S1
     * Y2S2
     * Y1T1
     * Y1M1
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /*
     * Human-readable period name.
     *
     * Examples:
     * Year 1 Semester 1
     * Year 2 Term 2
     * Year 1 Module 1
     */
    @Column(nullable = false, length = 100)
    private String name;

    /*
     * Identifies whether this is a semester, term,
     * trimester, module, quarter or block.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private AcademicPeriodType periodType;

    /*
     * The student's study year.
     *
     * Examples:
     * Y1S1 -> 1
     * Y2S1 -> 2
     * Y3T2 -> 3
     */
    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    /*
     * The period number inside the study year.
     *
     * Examples:
     * Y1S1 -> 1
     * Y1S2 -> 2
     * Y1T3 -> 3
     */
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    /*
     * Controls the order periods are displayed.
     *
     * For a semester structure:
     * Y1S1 -> 1
     * Y1S2 -> 2
     * Y2S1 -> 3
     * Y2S2 -> 4
     */
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        normalizeFields();

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        normalizeFields();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeFields() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }

        if (name != null) {
            name = name.trim();
        }
    }
}
