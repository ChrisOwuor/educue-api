package com.owuor.educue.results.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentResultFilterRequest {
    private String search;
    private Long courseId;
    private Long courseAcademicPeriodId;
    private Long courseUnitPlacementId;
    private String status;   // matches StudentResult.ResultStatus name
    private Boolean passed;
}
