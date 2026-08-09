package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdatePeriodFeeItemRequest(
        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        Long effectiveToIntakeId,

        @NotNull
        Boolean mandatory,

        @NotNull
        @PositiveOrZero
        Integer displayOrder
) {
}
