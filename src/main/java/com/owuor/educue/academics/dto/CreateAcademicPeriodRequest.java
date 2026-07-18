package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.AcademicPeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Data required to create a reusable academic period such as Y1S1. */
public record CreateAcademicPeriodRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull AcademicPeriodType periodType,
        @NotNull @Positive Integer yearNumber,
        @NotNull @Positive Integer periodNumber,
        @NotNull @Positive Integer sequenceNumber,
        Boolean active
) {}
