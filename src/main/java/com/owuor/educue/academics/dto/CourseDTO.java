package com.owuor.educue.academics.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CourseDTO {
    private Long id;
    private UUID uuid;
    private String code;
    private String name;
    private String departmentName;
    private Integer durationValue;
    private String durationUnit;
    private Integer totalSemesters;
    private boolean active;
}
