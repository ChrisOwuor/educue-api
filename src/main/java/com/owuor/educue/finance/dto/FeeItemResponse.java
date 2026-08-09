package com.owuor.educue.finance.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeeItemResponse(
        Long id,
        UUID uuid,
        String code,
        String name,
        String category,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
