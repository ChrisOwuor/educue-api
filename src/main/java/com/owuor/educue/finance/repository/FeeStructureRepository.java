package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.FeeStructure;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    boolean existsByIntakeCourseIdAndCourseAcademicPeriodId(
            Long intakeCourseId,
            Long courseAcademicPeriodId
    );

    @EntityGraph(attributePaths = {
            "intakeCourse.intake",
            "intakeCourse.course",
            "courseAcademicPeriod.academicPeriod",
            "items"
    })
    Optional<FeeStructure> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {
            "intakeCourse.intake",
            "intakeCourse.course",
            "courseAcademicPeriod.academicPeriod",
            "items"
    })
    Optional<FeeStructure> findByIntakeCourseIdAndCourseAcademicPeriodId(Long intakeCourseId, Long courseAcademicPeriodId);

    @EntityGraph(attributePaths = {"intakeCourse.intake", "intakeCourse.course", "courseAcademicPeriod.academicPeriod", "items"})
    List<FeeStructure> findByIntakeCourseIdOrderByCourseAcademicPeriodPosition(Long intakeCourseId);

    Optional<FeeStructure>findByIntakeCourseIdAndCourseAcademicPeriod(Long id, Long id1, Long id2);
}
