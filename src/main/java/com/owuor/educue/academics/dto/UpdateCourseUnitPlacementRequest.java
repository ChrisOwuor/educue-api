package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.UnitType;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** Editable placement fields; omitted properties remain unchanged. */
public record UpdateCourseUnitPlacementRequest(
        UUID courseAcademicPeriodUuid,
        UnitType unitType,
        @Positive Integer effectiveFromIntakeYear,
        @Positive Integer effectiveToIntakeYear,
        Boolean clearEffectiveToIntakeYear,
        Boolean active
) {}
