package com.owuor.educue.academics.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateCourseCurriculumRequest {

    private Long courseId;

    private String name;

    private Boolean active;

    private Boolean defaultForAdmission;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
