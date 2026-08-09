package com.owuor.educue.academics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateTrainerAssignmentRequest {

    @NotNull(message = "Lecturer is required.")
    private Long lecturerId;

    /**
     * Identifies the exact intake-versioned CourseUnitPlacement.
     */
    @NotNull(message = "Course unit placement is required.")
    private UUID courseUnitPlacementUuid;

    @NotNull(message = "Effective-from academic year is required.")
    private UUID effectiveFromAcademicYearUuid;

    /**
     * Inclusive final academic year.
     * Null keeps the lecturer assignment open-ended.
     */
    private UUID effectiveToAcademicYearUuid;
}
