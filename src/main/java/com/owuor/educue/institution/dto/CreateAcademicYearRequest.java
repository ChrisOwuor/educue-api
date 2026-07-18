package com.owuor.educue.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Data required to create an academic year. */
public record CreateAcademicYearRequest(
        @NotBlank @Size(max = 20) String code,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Boolean current,
        Boolean closed,
        Boolean active
) {}
