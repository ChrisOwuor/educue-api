package com.owuor.educue.graduation.dto;

import com.owuor.educue.academics.enums.QualificationType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GraduationFeeStructureResponse(
        QualificationType qualificationType, Long intakeId, String intakeName, Long intakeSequenceNumber,
        BigDecimal total, List<Item> items
) {
    public record Item(Long ruleId, UUID feeItemUuid, String feeItemCode, String feeItemName,
                       String feeItemCategory, BigDecimal amount, boolean inherited,
                       Long sourceIntakeId, String sourceIntakeName) {}
}
