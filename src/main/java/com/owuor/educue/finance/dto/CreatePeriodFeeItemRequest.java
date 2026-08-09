package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePeriodFeeItemRequest(
        @NotNull
        UUID courseAcademicPeriodUuid,

        @NotNull
        UUID feeItemUuid,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @NotNull
        Long effectiveFromIntakeId,

        Long effectiveToIntakeId,

        @NotNull
        Boolean mandatory,

        @NotNull
        @PositiveOrZero
        Integer displayOrder
) {
}
