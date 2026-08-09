package com.owuor.educue.students.dto;

import com.owuor.educue.academics.enums.UnitType;
import lombok.Builder;

@Builder
public record StudentUnitResponse(
                Long registrationId,
                Long courseUnitPlacementId,
                java.util.UUID courseUnitPlacementUuid,
                java.util.UUID courseAcademicPeriodUuid,
                Long unitId,
                String unitCode,
                String unitName,
                Integer creditHours,
                UnitType unitType,
                String academicPeriodCode,
                String academicPeriodName,
                String attemptType,
                String registrationOrigin

) {
}
