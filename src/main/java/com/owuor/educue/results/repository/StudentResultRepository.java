package com.owuor.educue.results.repository;

import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface StudentResultRepository
        extends JpaRepository<StudentResult, Long>, JpaSpecificationExecutor<StudentResult> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StudentResult r where r.id = :id")
    Optional<StudentResult> findByIdForAttemptRegistration(Long id);

    // Covers every relationship the HOD view's mapper touches - without
    // this, each result row would trigger a fresh round-trip to fetch
    // its registration, enrollment, student, placement, academic period,
    // course, recordedBy, and approvedBy individually (classic N+1).
    @Override
    @EntityGraph(attributePaths = {
            "studentUnitRegistration",
            "studentUnitRegistration.enrollment",
            "studentUnitRegistration.enrollment.student",
            "studentUnitRegistration.enrollment.currentCourseAcademicPeriod.academicPeriod",
            "studentUnitRegistration.enrollment.course",
            "studentUnitRegistration.courseUnitPlacement",
            "studentUnitRegistration.courseUnitPlacement.unit",
            "recordedBy",
            "approvedBy"
    })
    Page<StudentResult> findAll(Specification<StudentResult> spec, Pageable pageable);


    @EntityGraph(attributePaths = {
            "studentUnitRegistration"
    })
    Optional<StudentResult> findByStudentUnitRegistrationId(
            Long studentUnitRegistrationId
    );

    List<StudentResult>
    findByStudentUnitRegistrationCourseUnitPlacementId(
            Long courseUnitPlacementId
    );


    @EntityGraph(attributePaths = {
            "studentUnitRegistration",
            "studentUnitRegistration.courseUnitPlacement",
            "studentUnitRegistration.courseUnitPlacement.unit",
            "studentUnitRegistration.courseUnitPlacement.courseAcademicPeriod.academicPeriod",
            "studentUnitRegistration.enrollment",
            "studentUnitRegistration.enrollment.student"
    })
    List<StudentResult> findByStudentUnitRegistrationEnrollmentStudentUserIdOrderByStudentUnitRegistrationCourseUnitPlacementCourseAcademicPeriodPosition(
            Long userId
    );


    @EntityGraph(attributePaths = {
            "studentUnitRegistration"
    })
    List<StudentResult>
    findByStudentUnitRegistrationEnrollmentIdIn(
            List<Long> enrollmentIds
    );

    @EntityGraph(attributePaths = {"studentUnitRegistration", "studentUnitRegistration.courseUnitPlacement", "studentUnitRegistration.courseUnitPlacement.unit"})
    List<StudentResult> findByStudentUnitRegistrationEnrollmentId(Long enrollmentId);
    @Query("select count(r)>0 from StudentResult r where r.studentUnitRegistration.enrollment.id=:enrollmentId and r.studentUnitRegistration.courseUnitPlacement.id=:placementId and r.passed=true and r.status in (com.owuor.educue.results.enums.ResultStatus.APPROVED,com.owuor.educue.results.enums.ResultStatus.RELEASED)")
    boolean existsPassedAttempt(Long enrollmentId,Long placementId);



}
