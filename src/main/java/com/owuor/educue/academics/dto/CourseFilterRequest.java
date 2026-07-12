package com.owuor.educue.academics.dto;

import lombok.Data;

@Data
public class CourseFilterRequest {
    private int page = 0;
    private int size = 10;

    private String search;
    private String status; // active | inactive
    private Long departmentId;

    private String sort; // name,asc OR name,desc
}
