package com.owuor.educue.admissions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateIntakeRequest(
        @NotBlank String name,
        @NotNull UUID academicYearUuid,
        @NotNull LocalDate startDate,
        @NotNull LocalDate applicationDeadline
) {}
