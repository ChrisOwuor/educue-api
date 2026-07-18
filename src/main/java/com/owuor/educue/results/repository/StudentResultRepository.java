package com.owuor.educue.results.repository;

import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface StudentResultRepository
        extends JpaRepository<StudentResult, Long>, JpaSpecificationExecutor<StudentResult> {

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
            "studentUnitRegistration.enrollment.intakeCourse.course",
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



}
