package com.owuor.educue.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SavePeriodFeeStructureRequest(
        @NotNull
        UUID courseAcademicPeriodUuid,

        @NotNull
        Long intakeId,

        @NotNull
        @Valid
        List<Item> items
) {

    public record Item(
            @NotNull
            UUID feeItemUuid,

            @NotNull
            @DecimalMin(value = "0.00")
            @Digits(integer = 13, fraction = 2)
            BigDecimal amount
    ) {
    }
}
