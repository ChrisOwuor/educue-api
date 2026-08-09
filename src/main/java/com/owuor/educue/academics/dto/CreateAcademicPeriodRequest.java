package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.AcademicPeriodType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Generates the missing year/period matrix for one user-defined structure. */
public record CreateAcademicPeriodRequest(
        @NotNull AcademicPeriodType periodType,
        @NotNull @Positive @Max(20) Integer totalYears,
        @NotNull @Positive @Max(20) Integer periodsPerYear,
        @Positive @Max(20) Integer periodsInFinalYear,
        Boolean active
) {}
