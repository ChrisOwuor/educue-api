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
    // its registration, enrollment, student, unit, semester, curriculum,
    // course, recordedBy, and approvedBy individually (classic N+1).
    @Override
    @EntityGraph(attributePaths = {
            "studentUnitRegistration",
            "studentUnitRegistration.enrollment",
            "studentUnitRegistration.enrollment.student",
            "studentUnitRegistration.enrollment.currentSemester",
            "studentUnitRegistration.enrollment.courseCurriculum",
            "studentUnitRegistration.enrollment.courseCurriculum.course",
            "studentUnitRegistration.semesterUnit",
            "studentUnitRegistration.semesterUnit.unit",
            "studentUnitRegistration.semesterUnit.semester",
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
    findByStudentUnitRegistrationSemesterUnitId(
            Long semesterUnitId
    );


    @EntityGraph(attributePaths = {
            "studentUnitRegistration",
            "studentUnitRegistration.semesterUnit",
            "studentUnitRegistration.semesterUnit.unit",
            "studentUnitRegistration.semesterUnit.semester",
            "studentUnitRegistration.enrollment",
            "studentUnitRegistration.enrollment.student"
    })
    List<StudentResult> findByStudentUnitRegistrationEnrollmentStudentUserIdOrderByStudentUnitRegistrationSemesterUnitSemester(
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
