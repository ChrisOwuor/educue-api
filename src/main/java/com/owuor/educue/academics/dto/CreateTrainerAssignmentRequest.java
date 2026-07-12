package com.owuor.educue.academics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateTrainerAssignmentRequest {

    @NotNull(message = "Trainer is required.")
    private Long trainerId;

    @NotNull(message = "Semester unit is required.")
    private Long semesterUnitId;

    @NotNull(message = "Effective from date is required.")
    private LocalDate effectiveFrom;
}
