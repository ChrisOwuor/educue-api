package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.PeriodFeeItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodFeeItemRepository
        extends JpaRepository<PeriodFeeItem, Long>,
        JpaSpecificationExecutor<PeriodFeeItem> {

    /*
     * Loads one rule together with all relationships required by
     * PeriodFeeItemService and response mapping.
     */
    @EntityGraph(attributePaths = {
            "courseAcademicPeriod",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",
            "feeItem",
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    @Query("""
            select item
            from PeriodFeeItem item
            where item.id = :id
            """)
    Optional<PeriodFeeItem> findDetailedById(
            @Param("id") Long id
    );

    /*
     * Returns every historical version of one fee item for one
     * course academic period.
     *
     * Oldest version is returned first.
     */
    @EntityGraph(attributePaths = {
            "courseAcademicPeriod",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",
            "feeItem",
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    @Query("""
            select item
            from PeriodFeeItem item
            where item.courseAcademicPeriod.uuid = :courseAcademicPeriodUuid
              and item.feeItem.uuid = :feeItemUuid
            order by item.effectiveFromIntake.sequenceNumber asc,
                     item.id asc
            """)
    List<PeriodFeeItem> findHistory(
            @Param("courseAcademicPeriodUuid")
            UUID courseAcademicPeriodUuid,

            @Param("feeItemUuid")
            UUID feeItemUuid
    );

    /*
     * Checks whether the proposed half-open range [from, to)
     * overlaps another rule for the same course period and fee item.
     *
     * A null toSequence means the proposed range has no end.
     * A null excludedId means no record should be excluded.
     */
    @Query("""
            select count(item)
            from PeriodFeeItem item
            where item.courseAcademicPeriod.id = :courseAcademicPeriodId
              and item.feeItem.id = :feeItemId

              and (
                    :excludedId is null
                    or item.id <> :excludedId
              )

              and (
                    :toSequence is null
                    or item.effectiveFromIntake.sequenceNumber < :toSequence
              )

              and (
                    item.effectiveToIntake is null
                    or item.effectiveToIntake.sequenceNumber > :fromSequence
              )
            """)
    long countOverlappingRules(
            @Param("courseAcademicPeriodId")
            Long courseAcademicPeriodId,

            @Param("feeItemId")
            Long feeItemId,

            @Param("fromSequence")
            long fromSequence,

            @Param("toSequence")
            Long toSequence,

            @Param("excludedId")
            Long excludedId
    );

    @EntityGraph(attributePaths = {
            "effectiveFromIntake",
            "effectiveToIntake",
            "feeItem",
            "courseAcademicPeriod"
    })
    @Query("""
        select item
        from PeriodFeeItem item
        where item.courseAcademicPeriod.id = :courseAcademicPeriodId
          and item.feeItem.id = :feeItemId
          and item.effectiveFromIntake.sequenceNumber < :intakeSequence
        order by item.effectiveFromIntake.sequenceNumber desc
        limit 1
        """)
    Optional<PeriodFeeItem> findPredecessor(
            @Param("courseAcademicPeriodId")
            Long courseAcademicPeriodId,

            @Param("feeItemId")
            Long feeItemId,

            @Param("intakeSequence")
            long intakeSequence
    );

    @EntityGraph(attributePaths = {
            "courseAcademicPeriod",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",
            "feeItem",
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    @Query("""
            select p
            from PeriodFeeItem p
            where p.courseAcademicPeriod.uuid = :courseAcademicPeriodUuid
              and p.effectiveFromIntake.sequenceNumber <= :intakeSequence
              and (
                    p.effectiveToIntake is null
                    or p.effectiveToIntake.sequenceNumber > :intakeSequence
              )
            order by p.feeItem.name asc
            """)
    List<PeriodFeeItem> findEffectiveFees(
            @Param("courseAcademicPeriodUuid")
            UUID courseAcademicPeriodUuid,

            @Param("intakeSequence")
            Long intakeSequence
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "courseAcademicPeriod",
            "courseAcademicPeriod.course",
            "courseAcademicPeriod.academicPeriod",
            "feeItem",
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    @Query("""
            select p
            from PeriodFeeItem p
            where p.courseAcademicPeriod.uuid = :courseAcademicPeriodUuid
              and p.effectiveFromIntake.sequenceNumber <= :intakeSequence
              and (
                    p.effectiveToIntake is null
                    or p.effectiveToIntake.sequenceNumber > :intakeSequence
              )
            order by p.feeItem.id asc
            """)
    List<PeriodFeeItem> findEffectiveFeesForUpdate(
            @Param("courseAcademicPeriodUuid")
            UUID courseAcademicPeriodUuid,

            @Param("intakeSequence")
            Long intakeSequence
    );

    @EntityGraph(attributePaths = {
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    @Query("""
            select p
            from PeriodFeeItem p
            where p.courseAcademicPeriod.id = :courseAcademicPeriodId
              and p.feeItem.id = :feeItemId
              and p.effectiveFromIntake.sequenceNumber > :intakeSequence
            order by p.effectiveFromIntake.sequenceNumber asc
            limit 1
            """)
    Optional<PeriodFeeItem> findNextRule(
            @Param("courseAcademicPeriodId")
            Long courseAcademicPeriodId,

            @Param("feeItemId")
            Long feeItemId,

            @Param("intakeSequence")
            Long intakeSequence
    );

    @EntityGraph(attributePaths = {
            "effectiveFromIntake",
            "effectiveToIntake"
    })
    Optional<PeriodFeeItem>
    findFirstByCourseAcademicPeriod_IdAndFeeItem_IdAndEffectiveFromIntake_SequenceNumberGreaterThanOrderByEffectiveFromIntake_SequenceNumberAsc(
            Long courseAcademicPeriodId,
            Long feeItemId,
            Long intakeSequence
    );

}
