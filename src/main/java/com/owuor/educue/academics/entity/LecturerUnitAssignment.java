package com.owuor.educue.academics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.users.entity.User;

@Entity
@Table(name = "lecturer_unit_assignments", indexes = {
        @Index(name = "idx_lecturer_assignment_placement", columnList = "course_unit_placement_id"),
        @Index(name = "idx_lecturer_assignment_lecturer", columnList = "lecturer_id"),
        @Index(name = "idx_lecturer_assignment_effective_years", columnList = "effective_from_academic_year_id, effective_to_academic_year_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LecturerUnitAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    /*
     * Identifies the course, unit and academic period.
     *
     * Example:
     * BSc Computer Science + COMP102 + Y1S2
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_unit_placement_id", nullable = false)
    private CourseUnitPlacement courseUnitPlacement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    /*
     * First academic year in which this assignment applies.
     *
     * Example: 2026/2027
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "effective_from_academic_year_id", nullable = false)
    private AcademicYear effectiveFromAcademicYear;

    /*
     * Final academic year in which this assignment applies.
     *
     * NULL means the assignment has no defined ending year.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effective_to_academic_year_id")
    private AcademicYear effectiveToAcademicYear;


    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    /*
     * Records when the administrator performed the assignment.
     * This is different from the effective academic year.
     */
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /*
     * Records the user who created the assignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    
    /*
     * Allows an administrator to disable an assignment immediately
     * without changing its historical effective-year range.
     */
    @Column(nullable = false)
    private boolean enabled = true;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        LocalDateTime now = LocalDateTime.now();

        if (assignedAt == null) {
            assignedAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActiveFor(AcademicYear academicYear) {
        if (!enabled || academicYear == null) {
            return false;
        }

        int targetYear = academicYear.getStartYear();
        int fromYear = effectiveFromAcademicYear.getStartYear();

        if (targetYear < fromYear) {
            return false;
        }

        return effectiveToAcademicYear == null
                || targetYear <= effectiveToAcademicYear.getStartYear();
    }
}