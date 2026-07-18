package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Database access for course/unit/period placement records. */
public interface CourseUnitPlacementRepository extends JpaRepository<CourseUnitPlacement, Long> {

    /**
     * Placement-first allocation view. The graph avoids N+1 queries while
     * retaining placements that have no trainer assignment at all.
     */
    @EntityGraph(attributePaths = {
            "unit",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",
            "lecturerAssignments",
            "lecturerAssignments.lecturer",
            "lecturerAssignments.assignedBy",
            "lecturerAssignments.effectiveFromAcademicYear",
            "lecturerAssignments.effectiveToAcademicYear"
    })
    @Query("""
            select distinct placement
            from CourseUnitPlacement placement
            where placement.active = true
            order by placement.courseAcademicPeriod.course.name,
                     placement.courseAcademicPeriod.position,
                     placement.unit.code
            """)
    List<CourseUnitPlacement> findAllocationView();

    @EntityGraph(attributePaths = {"courseAcademicPeriod.course", "courseAcademicPeriod.academicPeriod", "unit"})
    Optional<CourseUnitPlacement> findByUuid(UUID uuid);

    boolean existsByCourseAcademicPeriodCourseIdAndUnitIdAndEffectiveFromIntakeYear(
            Long courseId,
            Long unitId,
            Integer effectiveFromIntakeYear
    );

    @EntityGraph(attributePaths = {"courseAcademicPeriod.course", "courseAcademicPeriod.academicPeriod", "unit"})
    @Query("""
            select placement from CourseUnitPlacement placement
            where placement.courseAcademicPeriod.course.uuid = :courseUuid
              and (:active is null or placement.active = :active)
              and (:intakeYear is null or (
                    placement.effectiveFromIntakeYear <= :intakeYear
                    and (placement.effectiveToIntakeYear is null
                         or placement.effectiveToIntakeYear >= :intakeYear)
              ))
            order by placement.courseAcademicPeriod.position, placement.unit.code
            """)
    List<CourseUnitPlacement> findForCourse(
            @Param("courseUuid") UUID courseUuid,
            @Param("intakeYear") Integer intakeYear,
            @Param("active") Boolean active
    );

    @EntityGraph(attributePaths = {"courseAcademicPeriod.academicPeriod", "unit"})
    @Query("""
            select placement from CourseUnitPlacement placement
            where placement.courseAcademicPeriod.id = :coursePeriodId
              and placement.active = true
              and placement.effectiveFromIntakeYear <= :intakeYear
              and (placement.effectiveToIntakeYear is null or placement.effectiveToIntakeYear >= :intakeYear)
            order by placement.unit.code
            """)
    List<CourseUnitPlacement> findActiveForPeriodAndIntakeYear(
            @Param("coursePeriodId") Long coursePeriodId,
            @Param("intakeYear") Integer intakeYear);
}
