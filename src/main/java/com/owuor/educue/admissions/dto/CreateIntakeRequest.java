package com.owuor.educue.admissions.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateIntakeRequest(
        @NotBlank String name,
        @NotNull UUID academicYearUuid,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate applicationDeadline,
        // Deliberately required and non-empty: an Intake with zero courses
        // attached is useless - the public apply page would show an admission
        // window with nothing to apply for. Force this decision at creation
        // time rather than allowing a silently broken intake.
        @NotEmpty List<Long> courseIds
) {}
