package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFeeItemRequest(

        @NotBlank(message = "Fee item name is required")
        @Size(max = 150, message = "Fee item name cannot exceed 150 characters")
        String name,

        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category
) {
}
