package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFeeItemRequest(

        @NotBlank(message = "Fee item code is required")
        @Size(max = 50, message = "Fee item code cannot exceed 50 characters")
        String code,

        @NotBlank(message = "Fee item name is required")
        @Size(max = 150, message = "Fee item name cannot exceed 150 characters")
        String name,

        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category
) {
}
