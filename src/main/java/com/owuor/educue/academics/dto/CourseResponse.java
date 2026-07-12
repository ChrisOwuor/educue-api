package com.owuor.educue.academics.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CourseResponse {
    private Long id;
    private UUID uuid;
    private String code;
    private String name;
    private Integer durationYears;
    private Integer totalSemesters;
    private String departmentName;
    private String durationUnit;
    private Integer durationValue;
}
