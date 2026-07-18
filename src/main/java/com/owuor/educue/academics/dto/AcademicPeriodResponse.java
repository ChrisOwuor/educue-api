package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.entity.AcademicPeriod;
import com.owuor.educue.academics.enums.AcademicPeriodType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Public API representation of an academic period. */
public record AcademicPeriodResponse(
        UUID uuid,
        String code,
        String name,
        AcademicPeriodType periodType,
        Integer yearNumber,
        Integer periodNumber,
        Integer sequenceNumber,
        boolean active,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AcademicPeriodResponse from(AcademicPeriod period) {
        return new AcademicPeriodResponse(
                period.getUuid(), period.getCode(), period.getName(), period.getPeriodType(),
                period.getYearNumber(), period.getPeriodNumber(), period.getSequenceNumber(),
                period.isActive(), period.getVersion(), period.getCreatedAt(), period.getUpdatedAt()
        );
    }
}
