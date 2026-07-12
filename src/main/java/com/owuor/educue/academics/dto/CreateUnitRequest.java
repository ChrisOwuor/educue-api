package com.owuor.educue.academics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUnitRequest {

    @NotNull
    private Long departmentId;

    @NotBlank
    private String name;

    private Integer creditHours;

    private String description;
}
