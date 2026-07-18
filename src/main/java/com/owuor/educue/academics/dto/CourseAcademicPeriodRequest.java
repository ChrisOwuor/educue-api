package com.owuor.educue.academics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CourseAcademicPeriodRequest(
        @NotNull UUID academicPeriodUuid,
        @NotNull @Positive Integer position
) {}
