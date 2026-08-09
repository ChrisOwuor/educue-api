package com.owuor.educue.finance.repository;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.finance.entity.GraduationFeeItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GraduationFeeItemRepository extends JpaRepository<GraduationFeeItem, Long> {
    @EntityGraph(attributePaths = {"feeItem", "effectiveFromIntake", "effectiveToIntake"})
    @Query("""
        select g from GraduationFeeItem g
        where g.qualificationType = :qualificationType
          and g.effectiveFromIntake.sequenceNumber <= :sequence
          and (g.effectiveToIntake is null or g.effectiveToIntake.sequenceNumber > :sequence)
        order by g.displayOrder, g.id
        """)
    List<GraduationFeeItem> findEffective(@Param("qualificationType") QualificationType qualificationType,
                                          @Param("sequence") Long sequence);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"feeItem", "effectiveFromIntake", "effectiveToIntake"})
    @Query("""
        select g from GraduationFeeItem g
        where g.qualificationType = :qualificationType
          and g.effectiveFromIntake.sequenceNumber <= :sequence
          and (g.effectiveToIntake is null or g.effectiveToIntake.sequenceNumber > :sequence)
        order by g.feeItem.id
        """)
    List<GraduationFeeItem> findEffectiveForUpdate(@Param("qualificationType") QualificationType qualificationType,
                                                   @Param("sequence") Long sequence);

    @EntityGraph(attributePaths = "effectiveFromIntake")
    @Query("""
        select g from GraduationFeeItem g
        where g.qualificationType = :qualificationType and g.feeItem.uuid = :feeItemUuid
          and g.effectiveFromIntake.sequenceNumber > :sequence
        order by g.effectiveFromIntake.sequenceNumber
        limit 1
        """)
    Optional<GraduationFeeItem> findNext(@Param("qualificationType") QualificationType qualificationType,
                                         @Param("feeItemUuid") UUID feeItemUuid,
                                         @Param("sequence") Long sequence);
}
