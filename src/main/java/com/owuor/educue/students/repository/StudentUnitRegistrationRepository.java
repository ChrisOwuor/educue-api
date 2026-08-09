package com.owuor.educue.students.repository;

import com.owuor.educue.academics.enums.RegistrationOrigin;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StudentUnitRegistrationRepository extends JpaRepository<StudentUnitRegistration, Long>,
                JpaSpecificationExecutor<StudentUnitRegistration> {

        boolean existsByEnrollmentIdAndCourseUnitPlacementIdAndStatus(
                        Long enrollmentId, Long placementId, RegistrationStatus status);

        boolean existsByEnrollmentIdAndCourseUnitPlacementIdAndAttemptTypeAndStatus(Long enrollmentId, Long placementId,
                        com.owuor.educue.academics.enums.AttemptType attemptType, RegistrationStatus status);

        boolean existsByEnrollmentIdAndCourseUnitPlacementIdAndAttemptTypeAndStatusAndIdNot(Long enrollmentId,
                        Long placementId, com.owuor.educue.academics.enums.AttemptType attemptType,
                        RegistrationStatus status, Long id);

        @Query("SELECT COUNT(r) > 0 FROM StudentUnitRegistration r WHERE r.enrollment.id = :enrollmentId AND r.courseUnitPlacement.id = :placementId AND r.attemptType <> com.owuor.educue.academics.enums.AttemptType.NORMAL AND r.status = com.owuor.educue.academics.enums.RegistrationStatus.ACTIVE")
        boolean existsActiveFurtherAttempt(@Param("enrollmentId") Long enrollmentId,
                        @Param("placementId") Long placementId);

        @EntityGraph(attributePaths = { "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByEnrollmentStudentUserIdAndStatusOrderByCourseUnitPlacementUnitCode(
                        Long userId, RegistrationStatus status);

        @EntityGraph(attributePaths = { "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByEnrollmentStudentUserIdAndStatusAndRegistrationOriginOrderByCourseUnitPlacementUnitCode(
                        Long userId, RegistrationStatus status, RegistrationOrigin registrationOrigin);

        @EntityGraph(attributePaths = { "courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod" })
        List<StudentUnitRegistration> findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodIdAndStatus(
                        Long enrollmentId, Long courseAcademicPeriodId, RegistrationStatus status);

        @EntityGraph(attributePaths = { "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByEnrollmentStudentIdAndCourseUnitPlacementCourseAcademicPeriodIdAndStatus(
                        Long studentId, Long courseAcademicPeriodId, RegistrationStatus status);

        @EntityGraph(attributePaths = { "courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod" })
        List<StudentUnitRegistration> findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodId(
                        Long enrollmentId, Long courseAcademicPeriodId);

        @EntityGraph(attributePaths = { "enrollment.student", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByCourseUnitPlacementIdAndStatusOrderByEnrollmentStudentAdmissionNumberAsc(
                        Long placementId, RegistrationStatus status);

        @EntityGraph(attributePaths = { "enrollment.student", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByCourseUnitPlacementIdAndStatusOrderByEnrollmentStudentFullNameAsc(
                        Long placementId, RegistrationStatus status);

        @EntityGraph(attributePaths = { "enrollment.student", "enrollment.course", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByCourseUnitPlacementIdAndStatusAndRegistrationOriginOrderByEnrollmentStudentFullNameAsc(
                        Long placementId, RegistrationStatus status, RegistrationOrigin registrationOrigin);

        @EntityGraph(attributePaths = { "enrollment.student", "enrollment.course", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.course",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByRegistrationOriginOrderByCourseUnitPlacementUnitCodeAsc(
                       RegistrationOrigin registrationOrigin);

        @EntityGraph(attributePaths = { "enrollment.student", "enrollment.course", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.course",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByCourseUnitPlacementUnitUuidAndStatusAndRegistrationOriginOrderByEnrollmentStudentFullNameAsc(
                        UUID unitUuid, RegistrationStatus status, RegistrationOrigin registrationOrigin);

        @EntityGraph(attributePaths = { "enrollment.student", "enrollment.course", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByCourseUnitPlacementIdInAndStatusOrderByEnrollmentStudentFullNameAsc(
                        List<Long> placementIds, RegistrationStatus status);

        @EntityGraph(attributePaths = { "enrollment.student", "enrollment.course",
                        "enrollment.currentCourseAcademicPeriod.academicPeriod", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByStatusOrderByRegisteredAtDesc(RegistrationStatus status);

        @Override
        @EntityGraph(attributePaths = { "enrollment.student", "enrollment.course",
                        "enrollment.currentCourseAcademicPeriod.academicPeriod", "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        Page<StudentUnitRegistration> findAll(Specification<StudentUnitRegistration> spec, Pageable pageable);

        @EntityGraph(attributePaths = { "courseUnitPlacement.unit",
                        "courseUnitPlacement.courseAcademicPeriod.academicPeriod" })
        List<StudentUnitRegistration> findByEnrollmentIdInAndStatus(List<Long> enrollmentIds,
                        RegistrationStatus status);
}
