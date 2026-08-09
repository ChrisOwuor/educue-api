package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.UnitType;

import java.util.List;
import java.util.UUID;

public record CourseUnitStructureResponse(

        UUID courseUuid,
        String courseCode,
        String courseName,

        UUID courseAcademicPeriodUuid,
        String academicPeriodCode,
        String academicPeriodName,

        Long intakeId,
        String intakeName,
        Long intakeSequenceNumber,

        List<Item> units,

        ChangeSummary changes
) {

    public record Item(

            Long placementId,
            UUID placementUuid,

            Long unitId,
            UUID unitUuid,
            String unitCode,
            String unitName,
            Integer creditHours,

            UnitType unitType,

            boolean inherited,

            Long sourceIntakeId,
            String sourceIntakeName
    ) {
    }

    public record ChangeSummary(
            int added,
            int typeChanged,
            int removed,
            int unchanged
    ) {

        public static ChangeSummary empty() {
            return new ChangeSummary(
                    0,
                    0,
                    0,
                    0
            );
        }
    }
}
