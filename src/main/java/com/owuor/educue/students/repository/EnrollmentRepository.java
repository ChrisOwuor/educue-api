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

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Long>,
        JpaSpecificationExecutor<Enrollment> {

    @Override
    @EntityGraph(attributePaths = {
            "student",
            "course",
            "currentSemester",
            "courseCurriculum"
    })
    Page<Enrollment> findAll(
            Specification<Enrollment> spec,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "course",
            "courseCurriculum",
            "currentSemester"
    })
    Optional<Enrollment> findByStudentUserId(Long userId);


    @EntityGraph(attributePaths = {
            "student",
            "currentSemester",
            "currentSemester.nextSemester"
    })
    Page<Enrollment> findByStatusAndCurrentSemesterId(
            EnrollmentStatus status,
            Long semesterId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "student",
            "currentSemester",
            "currentSemester.nextSemester"
    })
    Page<Enrollment> findByStatus(
            EnrollmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "student",
            "currentSemester",
            "courseCurriculum"
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
                JOIN e.student s
                JOIN s.application a
                JOIN FeeStructure fs
                    ON fs.course = e.course
                   AND fs.intake = a.intake
                   AND fs.semester = e.currentSemester
                WHERE e.id = :enrollmentId
            """)
    Optional<FeeStructure> findCurrentFeeStructure(Long enrollmentId);
}
