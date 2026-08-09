package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.students.dto.StudentUnitHistoryRow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseUnitPlacementRepository
        extends JpaRepository<CourseUnitPlacement, Long> {

    @Query("""
            select distinct placement
            from CourseUnitPlacement placement

            join fetch placement.unit unit
            join fetch placement.courseAcademicPeriod coursePeriod
            join fetch coursePeriod.course course
            join fetch coursePeriod.academicPeriod academicPeriod
            join fetch placement.effectiveFromIntake fromIntake
            left join fetch placement.effectiveToIntake toIntake

            where placement.uuid = :uuid
            """)
    Optional<CourseUnitPlacement> findDetailedByUuid(
            @Param("uuid") UUID uuid
    );

    /**
     * Finds placement versions effective for one intake.
     *
     * Range:
     * from <= selected < to
     */
    @Query("""
            select distinct placement
            from CourseUnitPlacement placement

            join fetch placement.unit unit
            join fetch placement.courseAcademicPeriod coursePeriod
            join fetch coursePeriod.course course
            join fetch coursePeriod.academicPeriod academicPeriod
            join fetch placement.effectiveFromIntake fromIntake
            left join fetch placement.effectiveToIntake toIntake

            where coursePeriod.id = :coursePeriodId
              and placement.active = true
              and unit.active = true
              and fromIntake.sequenceNumber <= :intakeSequence
              and (
                    toIntake is null
                    or toIntake.sequenceNumber > :intakeSequence
              )

            order by unit.code, placement.id
            """)
    List<CourseUnitPlacement>
    findEffectiveForPeriodAndIntakeSequence(
            @Param("coursePeriodId")
            Long coursePeriodId,

            @Param("intakeSequence")
            Long intakeSequence
    );

    /**
     * Finds the next future placement version for the same unit.
     */
    @EntityGraph(attributePaths = {
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    Optional<CourseUnitPlacement>
    findFirstByCourseAcademicPeriod_IdAndUnit_IdAndEffectiveFromIntake_SequenceNumberGreaterThanOrderByEffectiveFromIntake_SequenceNumberAsc(
            Long courseAcademicPeriodId,
            Long unitId,
            Long intakeSequence
    );

    /**
     * Used by lecturer allocation.
     */
    @EntityGraph(attributePaths = {
            "unit",
            "courseAcademicPeriod",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",

            "effectiveFromIntake",
            "effectiveToIntake",

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
              and placement.unit.active = true
              and placement.effectiveFromIntake.sequenceNumber <= :intakeSequence
              and (
                    placement.effectiveToIntake is null
                    or placement.effectiveToIntake.sequenceNumber > :intakeSequence
              )
            order by placement.courseAcademicPeriod.course.name,
                     placement.courseAcademicPeriod.position,
                     placement.unit.code
            """)
    List<CourseUnitPlacement>
    findAllocationViewForIntakeSequence(
            @Param("intakeSequence")
            Long intakeSequence
    );

    @EntityGraph(attributePaths = {
            "unit",
            "courseAcademicPeriod",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    @Query("""
            select placement
            from CourseUnitPlacement placement
            where placement.courseAcademicPeriod.uuid = :coursePeriodUuid
              and placement.unit.uuid = :unitUuid
            order by placement.effectiveFromIntake.sequenceNumber,
                     placement.id
            """)
    List<CourseUnitPlacement> findHistory(
            @Param("coursePeriodUuid")
            UUID coursePeriodUuid,

            @Param("unitUuid")
            UUID unitUuid
    );

    @EntityGraph(attributePaths = {
            "unit",

            "courseAcademicPeriod",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",

            "effectiveFromIntake",
            "effectiveToIntake",

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
          and placement.unit.active = true
        order by placement.courseAcademicPeriod.course.name,
                 placement.courseAcademicPeriod.position,
                 placement.unit.code,
                 placement.effectiveFromIntake.sequenceNumber
        """)
    List<CourseUnitPlacement> findAllForAllocationView();

    @EntityGraph(attributePaths = {"unit", "courseAcademicPeriod", "courseAcademicPeriod.academicPeriod"})
    @Query("""
        select p from CourseUnitPlacement p
        where p.courseAcademicPeriod.course.id = :courseId
          and p.active = true and p.unit.active = true
          and p.effectiveFromIntake.sequenceNumber <= :intakeSequence
          and (p.effectiveToIntake is null or p.effectiveToIntake.sequenceNumber > :intakeSequence)
        order by p.courseAcademicPeriod.position, p.unit.code
        """)
    List<CourseUnitPlacement> findEffectiveForCourseAndIntake(@Param("courseId") Long courseId, @Param("intakeSequence") Long intakeSequence);

    @Query("""
        select distinct placement
        from CourseUnitPlacement placement

        join fetch placement.unit unit
        join fetch placement.courseAcademicPeriod coursePeriod
        join fetch coursePeriod.course course
        join fetch coursePeriod.academicPeriod academicPeriod
        join fetch placement.effectiveFromIntake fromIntake
        left join fetch placement.effectiveToIntake toIntake

        where course.id = :courseId
          and coursePeriod.position <= :currentPosition
          and placement.active = true
          and unit.active = true
          and fromIntake.sequenceNumber <= :intakeSequence
          and (
                toIntake is null
                or toIntake.sequenceNumber > :intakeSequence
          )

        order by coursePeriod.position asc,
                 unit.code asc,
                 placement.id asc
        """)
    List<CourseUnitPlacement> findEffectiveUpToCurrentPeriod(
            @Param("courseId")
            Long courseId,

            @Param("currentPosition")
            Integer currentPosition,

            @Param("intakeSequence")
            Long intakeSequence
    );

    @Query("""
        select new com.owuor.educue.students.dto.StudentUnitHistoryRow(
            placement,
            registration
        )
        from CourseUnitPlacement placement

        join placement.unit unit
        join placement.courseAcademicPeriod coursePeriod
        join coursePeriod.course course
        join coursePeriod.academicPeriod academicPeriod
        join placement.effectiveFromIntake fromIntake
        left join placement.effectiveToIntake toIntake

        left join StudentUnitRegistration registration
            on registration.courseUnitPlacement.id = placement.id
           and registration.enrollment.id = :enrollmentId
           and registration.status = :registrationStatus

        where course.id = :courseId
          and coursePeriod.position <= :currentPosition
          and placement.active = true
          and unit.active = true
          and fromIntake.sequenceNumber <= :intakeSequence
          and (
                toIntake is null
                or toIntake.sequenceNumber > :intakeSequence
          )

        order by coursePeriod.position asc,
                 unit.code asc,
                 placement.id asc
        """)
    List<StudentUnitHistoryRow> findEffectiveHistoryForStudent(
            @Param("enrollmentId")
            Long enrollmentId,

            @Param("courseId")
            Long courseId,

            @Param("currentPosition")
            Integer currentPosition,

            @Param("intakeSequence")
            Long intakeSequence,

            @Param("registrationStatus")
            RegistrationStatus registrationStatus
    );



}
