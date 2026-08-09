package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.NotNull;

public record StopPeriodFeeItemRequest(
        @NotNull
        Long effectiveToIntakeId
) {
}
