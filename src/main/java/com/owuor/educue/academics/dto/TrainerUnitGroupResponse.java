package com.owuor.educue.academics.dto;

import java.util.List;
import java.util.UUID;

public record TrainerUnitGroupResponse(
        Long unitId,
        UUID unitUuid,
        String unitCode,
        String unitName,
        String academicYearCode,
        List<Placement> placements
) {
    public record Placement(
            Long courseUnitPlacementId,
            UUID courseUnitPlacementUuid,
            Long courseId,
            UUID courseUuid,
            String courseCode,
            String courseName,
            String academicPeriodCode,
            String academicPeriodName
    ) {}
}
