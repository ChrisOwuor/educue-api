package com.owuor.educue.academics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UnitDto {

    private Long id;

    private UUID uuid;

    private String code;

    private String name;

    private Integer creditHours;

    private String description;

    private Boolean active;

    private Long departmentId;

    private String departmentName;
}
