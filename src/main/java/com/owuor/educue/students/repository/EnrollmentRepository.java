package com.owuor.educue.students.repository;

import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.students.dto.PromotionStats;
import com.owuor.educue.students.dto.StudentPromotionRowResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Long>,
        JpaSpecificationExecutor<Enrollment> {

    @Override
    @EntityGraph(attributePaths = {
            "student",
            "intakeCourse.course",
            "intakeCourse.intake",
            "currentCourseAcademicPeriod.academicPeriod"
    })
    Page<Enrollment> findAll(
            Specification<Enrollment> spec,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "intakeCourse.course",
            "intakeCourse.intake",
            "currentCourseAcademicPeriod.academicPeriod"
    })
    Optional<Enrollment> findByStudentUserId(Long userId);

    @EntityGraph(attributePaths = {"student", "intakeCourse.course", "currentCourseAcademicPeriod.academicPeriod"})
    Optional<Enrollment> findByStudentId(Long studentId);


    @EntityGraph(attributePaths = {"student", "currentCourseAcademicPeriod", "currentCourseAcademicPeriod.nextPeriod"})
    Page<Enrollment> findByStatusAndCurrentCourseAcademicPeriodId(
            EnrollmentStatus status,
            Long courseAcademicPeriodId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "student",
            "currentCourseAcademicPeriod",
            "currentCourseAcademicPeriod.nextPeriod"
    })
    Page<Enrollment> findByStatus(
            EnrollmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "student",
            "currentCourseAcademicPeriod.academicPeriod",
            "currentCourseAcademicPeriod.nextPeriod",
            "intakeCourse.course"
    })
    @Query("""
            SELECT e
            FROM Enrollment e
            WHERE e.status = com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE
            AND (
                :search IS NULL
                OR lower(e.student.fullName) LIKE concat('%', :search, '%')
                OR lower(e.student.admissionNumber) LIKE concat('%', :search, '%')
            )
            """)
    Page<Enrollment> findPromotionCandidates(
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
                SELECT fs
                FROM Enrollment e
                JOIN FeeStructure fs
                    ON fs.intakeCourse = e.intakeCourse
                   AND fs.courseAcademicPeriod = e.currentCourseAcademicPeriod
                WHERE e.id = :enrollmentId
            """)
    Optional<FeeStructure> findCurrentFeeStructure(Long enrollmentId);

    /** Active students in one course at one exact progression period. */
    @EntityGraph(attributePaths = {
            "student",
            "intakeCourse.course",
            "intakeCourse.intake",
            "currentCourseAcademicPeriod.academicPeriod"
    })
    @Query("""
            SELECT e
            FROM Enrollment e
            WHERE e.status = com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE
              AND e.intakeCourse.course.uuid = :courseUuid
              AND e.currentCourseAcademicPeriod.uuid = :courseAcademicPeriodUuid
            ORDER BY e.student.admissionNumber, e.student.fullName
            """)
    List<Enrollment> findActiveClassList(
            @Param("courseUuid") UUID courseUuid,
            @Param("courseAcademicPeriodUuid") UUID courseAcademicPeriodUuid
    );
}
