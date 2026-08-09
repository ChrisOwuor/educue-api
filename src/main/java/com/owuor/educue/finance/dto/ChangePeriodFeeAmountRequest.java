package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChangePeriodFeeAmountRequest(
        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal newAmount,

        @NotNull
        Long effectiveFromIntakeId
) {
}
