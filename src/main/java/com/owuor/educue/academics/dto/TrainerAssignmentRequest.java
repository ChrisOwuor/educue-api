package com.owuor.educue.academics.dto;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
public class TrainerAssignmentRequest {

    @NotNull
    private Long semesterUnitId;

    @NotNull
    private Long trainerId;
}
