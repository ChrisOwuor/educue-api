package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.LecturerUnitAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LecturerUnitAssignmentRepository
        extends JpaRepository<LecturerUnitAssignment, Long> {

    @EntityGraph(attributePaths = {
            "lecturer",
            "assignedBy",
            "effectiveFromAcademicYear",
            "effectiveToAcademicYear",

            "courseUnitPlacement",
            "courseUnitPlacement.unit",

            "courseUnitPlacement.effectiveFromIntake",
            "courseUnitPlacement.effectiveToIntake",

            "courseUnitPlacement.courseAcademicPeriod",
            "courseUnitPlacement.courseAcademicPeriod.course",
            "courseUnitPlacement.courseAcademicPeriod.academicPeriod"
    })
    List<LecturerUnitAssignment>
    findByLecturerIdOrderByAssignedAtDesc(
            Long lecturerId
    );

    @EntityGraph(attributePaths = {
            "lecturer",
            "assignedBy",
            "effectiveFromAcademicYear",
            "effectiveToAcademicYear",

            "courseUnitPlacement",
            "courseUnitPlacement.unit",
            "courseUnitPlacement.effectiveFromIntake",
            "courseUnitPlacement.effectiveToIntake",

            "courseUnitPlacement.courseAcademicPeriod",
            "courseUnitPlacement.courseAcademicPeriod.course",
            "courseUnitPlacement.courseAcademicPeriod.academicPeriod"
    })
    List<LecturerUnitAssignment>
    findByCourseUnitPlacementIdAndEnabledTrueOrderByAssignedAtDesc(
            Long courseUnitPlacementId
    );




}
