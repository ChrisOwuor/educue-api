package com.owuor.educue.academics.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
public class CreateCurriculumWithStructureRequest {

    private Long courseId;

    private String name;

    private Boolean defaultForAdmission;

    private Integer totalYears;

    private Integer semestersPerYear;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
