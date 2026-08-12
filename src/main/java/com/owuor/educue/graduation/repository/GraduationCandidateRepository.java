package com.owuor.educue.graduation.repository;

import com.owuor.educue.graduation.entity.GraduationCandidate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface GraduationCandidateRepository extends JpaRepository<GraduationCandidate, Long>, JpaSpecificationExecutor<GraduationCandidate> {
    @EntityGraph(attributePaths = {"graduationList", "graduationList.academicYear", "enrollment", "enrollment.course", "enrollment.course.department"})
    List<GraduationCandidate> findByGraduationListAcademicYearUuidAndStatusOrderByEnrollmentCourseDepartmentNameAscGraduationNameAsc(java.util.UUID academicYearUuid, com.owuor.educue.graduation.enums.GraduationCandidateStatus status);
    @EntityGraph(attributePaths = {"enrollment", "enrollment.course"})
    Optional<GraduationCandidate> findFirstByEnrollmentIdOrderByGraduationListAcademicYearStartDateDesc(Long enrollmentId);

    @EntityGraph(attributePaths = {"graduationList", "graduationList.academicYear", "graduationList.department", "enrollment", "enrollment.student", "enrollment.course"})
    Optional<GraduationCandidate> findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(Long enrollmentId, Collection<com.owuor.educue.graduation.enums.GraduationCandidateStatus> statuses);

    Optional<GraduationCandidate> findByGraduationListIdAndEnrollmentId(Long listId, Long enrollmentId);

    boolean existsByEnrollmentIdAndStatusIn(Long enrollmentId, Collection<com.owuor.educue.graduation.enums.GraduationCandidateStatus> statuses);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.course",
            "enrollment.course.department",
            "enrollment.intake",
            "academicApprovalBy"
    })
    @Query("""
                SELECT application
                FROM GraduationCandidate application
                WHERE application.id = :candidateId
            """)
    Optional<GraduationCandidate> findDetailedById(
            @Param("candidateId") Long candidateId
    );

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.course"
    })
    @Override
    Page<GraduationCandidate> findAll(
            Specification<GraduationCandidate> specification,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT application
                FROM GraduationCandidate application
                WHERE application.id = :candidateId
            """)
    Optional<GraduationCandidate> findDetailedByIdForUpdate(
            @Param("candidateId") Long candidateId
    );

}
