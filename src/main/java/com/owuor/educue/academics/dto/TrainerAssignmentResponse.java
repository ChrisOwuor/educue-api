package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.UnitType;
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
    private UUID unitUuid;
    private String unitCode;
    private String unitName;
    private UnitType unitType;

    private Long courseAcademicPeriodId;
    private UUID courseAcademicPeriodUuid;
    private String academicPeriodCode;
    private String academicPeriodName;

    private Long courseId;
    private UUID courseUuid;
    private String courseCode;
    private String courseName;

    /*
     * Student-cohort applicability of the placement.
     */
    private Long placementEffectiveFromIntakeId;
    private String placementEffectiveFromIntakeName;

    private Long placementEffectiveToIntakeId;
    private String placementEffectiveToIntakeName;

    /*
     * Lecturer assignment applicability.
     */
    private UUID effectiveFromAcademicYearUuid;
    private String effectiveFrom;

    private UUID effectiveToAcademicYearUuid;
    private String effectiveTo;

    private boolean active;

    private LocalDateTime assignedAt;

    private Long assignedById;
    private String assignedByName;
}
