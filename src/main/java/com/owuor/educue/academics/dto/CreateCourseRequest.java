package com.owuor.educue.academics.dto;

import lombok.Data;

@Data
public class CreateCourseRequest {
    private Long departmentId;
    private String name;
    private String durationUnit;
    private Integer durationValue;
    private Integer totalSemesters;
}
