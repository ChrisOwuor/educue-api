package com.owuor.educue.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateFeeStructureRequest {

    @NotNull
    private Long intakeId;

    @NotNull
    private Long courseId;

    @NotNull
    private Long semesterId;

    @Valid
    @NotEmpty
    private List<CreateFeeStructureItemRequest> items;

}
