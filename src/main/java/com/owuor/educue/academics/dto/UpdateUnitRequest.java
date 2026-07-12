package com.owuor.educue.academics.dto;

import lombok.Data;

@Data
public class UpdateUnitRequest {

    private Long departmentId;

    private String code;

    private String name;

    private Integer creditHours;

    private String description;

    private Boolean active;
}
