package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CourseUnitAllocationResponse {

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
     * Student intake applicability of this placement version.
     */
    private Long placementEffectiveFromIntakeId;
    private String placementEffectiveFromIntakeName;

    private Long placementEffectiveToIntakeId;
    private String placementEffectiveToIntakeName;

    /*
     * Current academic-year assignment.
     * These fields are null when the placement is not allocated.
     */
    private Long assignmentId;

    private Long lecturerId;
    private String lecturerName;

    private UUID effectiveFromAcademicYearUuid;
    private String effectiveFromAcademicYearCode;

    private UUID effectiveToAcademicYearUuid;
    private String effectiveToAcademicYearCode;

    private LocalDateTime assignedAt;

    private Long assignedById;
    private String assignedByName;
}
