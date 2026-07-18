package com.owuor.educue.students.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentFilterRequest {

    private int page = 0;

    private int size = 3;

    private String sort = "id,desc";

    private String search;

    private Long courseId;

    private Long courseAcademicPeriodId;

    private String status;
}
