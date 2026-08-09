package com.owuor.educue.academics.entity;

import com.owuor.educue.academics.enums.UnitType;
import com.owuor.educue.admissions.entity.Intake;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "course_unit_placements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_unit_placement_start",
                        columnNames = {
                                "course_academic_period_id",
                                "unit_id",
                                "effective_from_intake_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_course_unit_placement_period",
                        columnList = "course_academic_period_id"
                ),
                @Index(
                        name = "idx_course_unit_placement_unit",
                        columnList = "unit_id"
                ),
                @Index(
                        name = "idx_course_unit_placement_range",
                        columnList = "effective_from_intake_id,effective_to_intake_id"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class CourseUnitPlacement {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(
                nullable = false,
                unique = true,
                updatable = false
        )
        private UUID uuid;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(
                name = "course_academic_period_id",
                nullable = false
        )
        private CourseAcademicPeriod courseAcademicPeriod;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(
                name = "unit_id",
                nullable = false
        )
        private Unit unit;

        @Enumerated(EnumType.STRING)
        @Column(
                name = "unit_type",
                nullable = false,
                length = 20
        )
        private UnitType unitType;

        /**
         * Inclusive starting intake.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(
                name = "effective_from_intake_id",
                nullable = false
        )
        private Intake effectiveFromIntake;

        /**
         * Exclusive ending intake.
         *
         * Null means the placement continues indefinitely.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "effective_to_intake_id")
        private Intake effectiveToIntake;

        @Column(nullable = false)
        private boolean active = true;

        @OneToMany(
                mappedBy = "courseUnitPlacement",
                fetch = FetchType.LAZY
        )
        @OrderBy("assignedAt DESC")
        private List<LecturerUnitAssignment> lecturerAssignments =
                new ArrayList<>();

        @Version
        @Column(nullable = false)
        private Long version;

        @CreatedDate
        @Column(
                name = "created_at",
                nullable = false,
                updatable = false
        )
        private LocalDateTime createdAt;

        @LastModifiedDate
        @Column(
                name = "updated_at",
                nullable = false
        )
        private LocalDateTime updatedAt;

        @PrePersist
        void assignUuid() {
                if (uuid == null) {
                        uuid = UUID.randomUUID();
                }
        }
}
