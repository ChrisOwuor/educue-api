package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.UnitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** One unit selected on the course distribution screen. */
public record CourseUnitPlacementItemRequest(
        @NotNull UUID unitUuid,
        @NotNull UUID courseAcademicPeriodUuid,
        @NotNull UnitType unitType,
        @NotNull @Positive Integer effectiveFromIntakeYear,
        @Positive Integer effectiveToIntakeYear,
        Boolean active
) {}
