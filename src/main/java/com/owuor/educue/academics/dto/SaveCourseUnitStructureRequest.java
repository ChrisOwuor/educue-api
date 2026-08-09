package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.UnitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SaveCourseUnitStructureRequest(

        @NotNull
        UUID courseAcademicPeriodUuid,

        @NotNull
        Long intakeId,

        @NotNull
        @Valid
        List<Item> units
) {

    public record Item(

            @NotNull
            UUID unitUuid,

            @NotNull
            UnitType unitType
    ) {
    }
}
