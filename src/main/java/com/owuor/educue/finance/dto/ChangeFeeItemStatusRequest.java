package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeFeeItemStatusRequest(

        @NotNull(message = "Active status is required")
        Boolean active
) {
}
