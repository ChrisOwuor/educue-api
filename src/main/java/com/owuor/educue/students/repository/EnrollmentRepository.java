package com.owuor.educue.students.repository;

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
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Long>,
        JpaSpecificationExecutor<Enrollment> {

    Optional<Enrollment> findByUuid(UUID uuid);

    @Override
    @EntityGraph(attributePaths = {
            "student",
            "course",
            "intake",
            "enrolledAcademicYear",
            "currentAcademicYear",
            "currentCourseAcademicPeriod.academicPeriod"
    })
    Page<Enrollment> findAll(
            Specification<Enrollment> spec,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "course",
            "intake",
            "enrolledAcademicYear",
            "currentAcademicYear",
            "currentCourseAcademicPeriod.academicPeriod"
    })
    Optional<Enrollment> findByStudentUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "student", "course", "intake", "currentCourseAcademicPeriod",
            "currentCourseAcademicPeriod.academicPeriod", "currentCourseAcademicPeriod.nextPeriod"
    })
    @Query("select e from Enrollment e where e.student.user.id = :userId")
    Optional<Enrollment> findByStudentUserIdForUpdate(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"student", "course", "intake", "enrolledAcademicYear", "currentAcademicYear", "currentCourseAcademicPeriod.academicPeriod"})
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
            "course",
            "intake",
            "currentAcademicYear"
    })
    @Query("""
        SELECT e
        FROM Enrollment e
        WHERE e.status =
            com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE
        AND (
            :search = ''
            OR lower(e.student.fullName)
                LIKE concat('%', :search, '%')
            OR lower(e.student.admissionNumber)
                LIKE concat('%', :search, '%')
            OR lower(e.course.name)
                LIKE concat('%', :search, '%')
            OR lower(e.course.code)
                LIKE concat('%', :search, '%')
            OR lower(
                e.currentCourseAcademicPeriod.academicPeriod.name
            ) LIKE concat('%', :search, '%')
        )
        """)
    Page<Enrollment> findPromotionCandidates(
            @Param("search") String search,
            Pageable pageable
    );

    /** Active students in one course at one exact progression period. */
    @EntityGraph(attributePaths = {
            "student",
            "course",
            "intake",
            "currentCourseAcademicPeriod.academicPeriod"
    })
    @Query("""
            SELECT e
            FROM Enrollment e
            WHERE e.status = com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE
              AND e.course.uuid = :courseUuid
              AND e.currentCourseAcademicPeriod.uuid = :courseAcademicPeriodUuid
            ORDER BY e.student.admissionNumber, e.student.fullName
            """)
    List<Enrollment> findActiveClassList(
            @Param("courseUuid") UUID courseUuid,
            @Param("courseAcademicPeriodUuid") UUID courseAcademicPeriodUuid
    );
}
