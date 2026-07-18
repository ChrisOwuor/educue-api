package com.owuor.educue.institution.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Fields that may be changed on an academic year; omitted fields remain unchanged. */
public record UpdateAcademicYearRequest(
        @Size(min = 1, max = 20) String code,
        LocalDate startDate,
        LocalDate endDate,
        Boolean current,
        Boolean closed,
        Boolean active
) {}
