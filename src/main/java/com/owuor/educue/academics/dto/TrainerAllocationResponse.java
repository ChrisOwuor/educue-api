package com.owuor.educue.academics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TrainerAllocationResponse {

    private Long semesterUnitId;

    private Long unitId;
    private String unitCode;
    private String unitName;

    private Long semesterId;
    private String semesterName;

    private Long curriculumId;
    private String curriculumName;

    private Long courseId;
    private String courseName;

    private Long trainerAssignmentId;

    private Long trainerId;
    private String trainerName;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    private boolean assigned;
}
