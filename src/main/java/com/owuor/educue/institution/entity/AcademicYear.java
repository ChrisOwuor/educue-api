package com.owuor.educue.institution.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "academic_years",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_academic_years_code",
            columnNames = {"code"}
        )
    },
    indexes = {
        @Index(
            name = "idx_academic_years_start_year",
            columnList = "start_year"
        ),
        @Index(
            name = "idx_academic_years_current",
            columnList = "current"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    /*
     * Example: 2026/2027
     */
    @Column(nullable = false, length = 20)
    private String code;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /*
     * Only one academic year should normally be current
     * for an institution.
     */
    @Column(nullable = false)
    private boolean current = false;

    /*
     * Prevents registration and assignments from being
     * changed after the academic year has been finalized.
     */
    @Column(nullable = false)
    private boolean closed = false;

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

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        normalize();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }
}
