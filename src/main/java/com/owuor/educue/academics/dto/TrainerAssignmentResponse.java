package com.owuor.educue.academics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TrainerAssignmentResponse {

    private Long id;

    private Long trainerId;
    private String trainerName;

    private Long courseUnitPlacementId;
    private UUID courseUnitPlacementUuid;

    private Long unitId;
    private String unitCode;
    private String unitName;

    private Long courseAcademicPeriodId;
    private String academicPeriodName;

    private Long courseId;
    private String courseName;

    private UUID effectiveFromAcademicYearUuid;
    private String effectiveFrom;
    private UUID effectiveToAcademicYearUuid;
    private String effectiveTo;

    private boolean active;

    private LocalDateTime assignedAt;

    private Long assignedById;
    private String assignedByName;
}
