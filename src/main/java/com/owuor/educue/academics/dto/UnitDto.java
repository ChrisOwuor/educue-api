package com.owuor.educue.academics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnitDto {

    private Long id;

    private String code;

    private String name;

    private Integer creditHours;

    private String description;

    private Boolean active;

    private Long departmentId;

    private String departmentName;
}
