package com.owuor.educue.academics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_curriculums")
@Getter
@Setter
@NoArgsConstructor
public class CourseCurriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 150)
    private String name; // e.g. "ICT Curriculum 2026"

    // Means "still has live students referencing it" - NEVER set false
    // just because a newer curriculum exists. Only set false once every
    // Enrollment using it has graduated/withdrawn.
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // The fix for the "which curriculum does a NEW admission get" gap:
    // exactly one curriculum per Course should have this true at any time.
    // This is what the enrollment form reads from - distinct from
    // `active`, which only means "don't break existing references."
    @Column(name = "is_default_for_admission", nullable = false)
    private boolean defaultForAdmission = false;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
   //TODO(SQSL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_semester_id")
    private Semester firstSemester;
}
