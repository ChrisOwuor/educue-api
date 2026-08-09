package com.owuor.educue.results.dto;

import java.util.List;
import java.util.UUID;

public record LegacyUnitGroupResponse(
        Long unitId,
        UUID unitUuid,
        String unitCode,
        String unitName,
        long totalRegistered,
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
            String academicPeriodName,
            long registeredStudents
    ) {}
}
