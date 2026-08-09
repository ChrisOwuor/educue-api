package com.owuor.educue.students.entity;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.students.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intake_id", nullable = false)
    private Intake intake;

    /** Permanent record of the academic year in which the student joined. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrolled_academic_year_id", nullable = false)
    private AcademicYear enrolledAcademicYear;

    /** Operational academic year used for current registration and billing. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_academic_year_id", nullable = false)
    private AcademicYear currentAcademicYear;

    /**
     * The student's current position in the course progression chain.
     * Supports semesters, terms, modules, trimesters and blocks uniformly.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_course_academic_period_id", nullable = false)
    private CourseAcademicPeriod currentCourseAcademicPeriod;

    /** Permanent cohort pricing contract selected when the enrollment is created. */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
