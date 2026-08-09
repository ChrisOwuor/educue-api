package com.owuor.educue.graduation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GraduationApplicationResponse(
        Long applicationId, Long enrollmentId, String courseCode, String qualificationType,
        String status, BigDecimal total, String ledgerDocumentNumber, LocalDateTime appliedAt,
        List<Item> items
) {
    public record Item(UUID feeItemUuid, String code, String name, BigDecimal amount, int displayOrder) {}
}
