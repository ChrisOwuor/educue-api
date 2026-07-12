package com.owuor.educue.academics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CourseCurriculumResponse {

    private Long id;

    private Long courseId;

    private String courseName;

    private String name;

    private boolean active;

    private boolean defaultForAdmission;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
