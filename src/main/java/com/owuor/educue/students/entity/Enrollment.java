package com.owuor.educue.students.entity;

import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.students.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Locks the student to one specific curriculum version. A new cohort
    // admitted after this curriculum is retired/replaced gets enrolled
    // against a DIFFERENT CourseCurriculum row for the same Course -
    // this is what lets curricula change without disturbing existing students.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_curriculum_id", nullable = false)
    private CourseCurriculum courseCurriculum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Tracks exactly where the student is academically. Promotion logic
    // must only ever move this to the NEXT Semester within the SAME
    // course_curriculum_id - never an unscoped "next semester_number"
    // query, since many curricula share overlapping semester numbers.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_semester_id", nullable = false)
    private Semester currentSemester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
