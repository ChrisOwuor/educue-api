package com.owuor.educue.students.entity;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.admissions.entity.IntakeCourse;
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

    /**
     * The concrete course offering through which the student was admitted.
     * This is the authoritative source for both the intake and course.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intake_course_id", nullable = false)
    private IntakeCourse intakeCourse;

    /**
     * The student's current position in the course progression chain.
     * Supports semesters, terms, modules, trimesters and blocks uniformly.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_course_academic_period_id", nullable = false)
    private CourseAcademicPeriod currentCourseAcademicPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
