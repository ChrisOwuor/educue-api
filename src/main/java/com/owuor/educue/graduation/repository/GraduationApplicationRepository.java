package com.owuor.educue.graduation.repository;

import com.owuor.educue.graduation.entity.GraduationApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface GraduationApplicationRepository extends JpaRepository<GraduationApplication, Long>, JpaSpecificationExecutor<GraduationApplication> {
    @EntityGraph(attributePaths = {"graduationList", "graduationList.academicYear", "enrollment", "enrollment.course", "enrollment.course.department"})
    List<GraduationApplication> findByGraduationListAcademicYearUuidAndStatusOrderByEnrollmentCourseDepartmentNameAscGraduationNameAsc(java.util.UUID academicYearUuid, com.owuor.educue.graduation.enums.GraduationApplicationStatus status);
    @EntityGraph(attributePaths = {"enrollment", "enrollment.course", "feeItems", "ledgerEntry"})
    Optional<GraduationApplication> findFirstByEnrollmentIdOrderByGraduationListAcademicYearStartDateDesc(Long enrollmentId);

    @EntityGraph(attributePaths = {"graduationList", "graduationList.academicYear", "enrollment", "enrollment.student", "enrollment.course", "feeItems", "ledgerEntry"})
    Optional<GraduationApplication> findFirstByEnrollmentIdAndStatusNotOrderByGraduationListAcademicYearStartDateDesc(Long enrollmentId, com.owuor.educue.graduation.enums.GraduationApplicationStatus status);

    @EntityGraph(attributePaths = {"graduationList", "graduationList.academicYear", "graduationList.department", "enrollment", "enrollment.student", "enrollment.course", "feeItems", "ledgerEntry"})
    Optional<GraduationApplication> findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(Long enrollmentId, Collection<com.owuor.educue.graduation.enums.GraduationApplicationStatus> statuses);

    Optional<GraduationApplication> findByGraduationListIdAndEnrollmentId(Long listId, Long enrollmentId);

    boolean existsByEnrollmentIdAndStatusIn(Long enrollmentId, Collection<com.owuor.educue.graduation.enums.GraduationApplicationStatus> statuses);

    @EntityGraph(attributePaths = {"enrollment", "enrollment.student", "enrollment.course", "feeItems", "ledgerEntry"})
    List<GraduationApplication> findAllByOrderByAppliedAtDesc();

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.course",
            "enrollment.course.department",
            "enrollment.intake",
            "ledgerEntry",
            "feeItems",
            "academicApprovalBy"
    })
    @Query("""
                SELECT application
                FROM GraduationApplication application
                WHERE application.id = :applicationId
            """)
    Optional<GraduationApplication> findDetailedById(
            @Param("applicationId") Long applicationId
    );

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.course"
    })
    @Override
    Page<GraduationApplication> findAll(
            Specification<GraduationApplication> specification,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.course",
            "enrollment.course.department",
            "enrollment.intake",
            "ledgerEntry",
            "feeItems",
            "academicApprovalBy"
    })
    @Query("""
                SELECT application
                FROM GraduationApplication application
                WHERE application.id = :applicationId
            """)
    Optional<GraduationApplication> findDetailedByIdForUpdate(
            @Param("applicationId") Long applicationId
    );

}
