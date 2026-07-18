package com.owuor.educue.students.repository;

import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StudentUnitRegistrationRepository extends JpaRepository<StudentUnitRegistration, Long>,
        JpaSpecificationExecutor<StudentUnitRegistration> {

    boolean existsByEnrollmentIdAndCourseUnitPlacementIdAndStatus(
            Long enrollmentId, Long placementId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod.academicPeriod"})
    List<StudentUnitRegistration> findByEnrollmentStudentUserIdAndStatusOrderByCourseUnitPlacementUnitCode(
            Long userId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod"})
    List<StudentUnitRegistration> findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodIdAndStatus(
            Long enrollmentId, Long courseAcademicPeriodId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod.academicPeriod"})
    List<StudentUnitRegistration> findByEnrollmentStudentIdAndCourseUnitPlacementCourseAcademicPeriodIdAndStatus(
            Long studentId, Long courseAcademicPeriodId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod"})
    List<StudentUnitRegistration> findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodId(
            Long enrollmentId, Long courseAcademicPeriodId);

    @EntityGraph(attributePaths = {"enrollment.student", "courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod.academicPeriod"})
    List<StudentUnitRegistration> findByCourseUnitPlacementIdAndStatusOrderByEnrollmentStudentAdmissionNumberAsc(
            Long placementId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"enrollment.student", "courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod.academicPeriod"})
    List<StudentUnitRegistration> findByCourseUnitPlacementIdAndStatusOrderByEnrollmentStudentFullNameAsc(
            Long placementId, RegistrationStatus status);

    @EntityGraph(attributePaths = {"enrollment.student", "enrollment.intakeCourse.course",
            "enrollment.currentCourseAcademicPeriod.academicPeriod", "courseUnitPlacement.unit",
            "courseUnitPlacement.courseAcademicPeriod.academicPeriod"})
    List<StudentUnitRegistration> findByStatusOrderByRegisteredAtDesc(RegistrationStatus status);

    @Override
    @EntityGraph(attributePaths = {"enrollment.student", "enrollment.intakeCourse.course",
            "enrollment.currentCourseAcademicPeriod.academicPeriod", "courseUnitPlacement.unit",
            "courseUnitPlacement.courseAcademicPeriod.academicPeriod"})
    Page<StudentUnitRegistration> findAll(Specification<StudentUnitRegistration> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"courseUnitPlacement.unit", "courseUnitPlacement.courseAcademicPeriod.academicPeriod"})
    List<StudentUnitRegistration> findByEnrollmentIdInAndStatus(List<Long> enrollmentIds, RegistrationStatus status);
}
