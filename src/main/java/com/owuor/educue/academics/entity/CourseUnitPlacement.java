package com.owuor.educue.academics.entity;

import com.owuor.educue.academics.enums.UnitType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_unit_placements", uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_unit_placement_start", columnNames = {
                                "course_academic_period_id",
                                "unit_id",
                                "effective_from_intake_year"
                })
}, indexes = {
                @Index(name = "idx_course_unit_placement_course_period", columnList = "course_academic_period_id"),
                @Index(name = "idx_course_unit_placement_unit", columnList = "unit_id"),
                @Index(name = "idx_course_unit_placement_intake_range", columnList = "effective_from_intake_year, effective_to_intake_year")
})
@Getter
@Setter
@NoArgsConstructor
public class CourseUnitPlacement {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        // Public-safe identifier
        @Column(nullable = false, unique = true, updatable = false)
        private UUID uuid;

        /** The course-specific stage to which this unit is assigned. */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "course_academic_period_id", nullable = false)
        private CourseAcademicPeriod courseAcademicPeriod;
        /*
         * The academic unit/subject.
         *
         * Example:
         * COMP102 - Object-Oriented Programming
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "unit_id", nullable = false)
        private Unit unit;

        /**
         * Allocation history for this placed unit. Repository entity graphs load
         * this only for allocation screens; normal placement reads stay lazy.
         */
        @OneToMany(mappedBy = "courseUnitPlacement", fetch = FetchType.LAZY)
        @OrderBy("assignedAt DESC")
        private List<LecturerUnitAssignment> lecturerAssignments = new ArrayList<>();

        /*
         * A unit may be CORE in one course but ELECTIVE
         * in another course.
         */
        @Enumerated(EnumType.STRING)
        @Column(name = "unit_type", nullable = false, length = 20)
        private UnitType unitType;

        /*
         * The first intake year that follows this placement.
         *
         * Example:
         * 2025 means students admitted in the 2025 intake.
         */
        @Column(name = "effective_from_intake_year", nullable = false)
        private Integer effectiveFromIntakeYear;

        /*
         * The final intake year that follows this placement.
         *
         * NULL means the placement still applies to future intakes.
         */
        @Column(name = "effective_to_intake_year")
        private Integer effectiveToIntakeYear;

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
        }

        @PreUpdate
        public void onUpdate() {
                updatedAt = LocalDateTime.now();
        }
}
