package com.owuor.educue.institution.dto;

import com.owuor.educue.institution.entity.AcademicYear;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Public representation of an academic year. */
public record AcademicYearResponse(
        UUID uuid,
        String code,
        LocalDate startDate,
        Integer startYear,
        LocalDate endDate,
        boolean current,
        boolean closed,
        boolean active,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AcademicYearResponse from(AcademicYear year) {
        return new AcademicYearResponse(
                year.getUuid(), year.getCode(), year.getStartDate(), year.getStartYear(),
                year.getEndDate(), year.isCurrent(), year.isClosed(), year.isActive(),
                year.getVersion(), year.getCreatedAt(), year.getUpdatedAt()
        );
    }
}
