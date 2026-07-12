package com.owuor.educue.academics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TrainerAssignmentResponse {

    private Long id;

    private Long trainerId;
    private String trainerName;

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

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private boolean active;

    private LocalDateTime assignedAt;

    private Long assignedById;
    private String assignedByName;
}
