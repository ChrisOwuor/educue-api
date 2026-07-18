package com.owuor.educue.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateFeeStructureRequest {

    @NotNull
    private Long intakeCourseId;

    @NotNull
    private UUID courseAcademicPeriodUuid;

    @Valid
    @NotEmpty
    private List<CreateFeeStructureItemRequest> items;

}
