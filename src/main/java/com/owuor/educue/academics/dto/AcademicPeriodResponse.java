package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.entity.AcademicPeriod;
import com.owuor.educue.academics.enums.AcademicPeriodType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Public API representation of an academic period. */
public record AcademicPeriodResponse(
        Long id,
        UUID uuid,
        String code,
        String name,
        AcademicPeriodType periodType,
        Integer yearNumber,
        Integer periodNumber,
        Integer periodsPerYear,
        Integer sequenceNumber,
        boolean active,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AcademicPeriodResponse from(AcademicPeriod period) {
        return new AcademicPeriodResponse(
                period.getId(),period.getUuid(), period.getCode(), period.getName(), period.getPeriodType(),
                period.getYearNumber(), period.getPeriodNumber(), period.getPeriodsPerYear(), period.getSequenceNumber(),
                period.isActive(), period.getVersion(), period.getCreatedAt(), period.getUpdatedAt()
        );
    }
}
