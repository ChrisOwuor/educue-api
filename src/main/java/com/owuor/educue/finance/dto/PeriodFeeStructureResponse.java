package com.owuor.educue.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PeriodFeeStructureResponse(
        UUID courseAcademicPeriodUuid,

        Long courseId,
        String courseCode,
        String courseName,

        UUID academicPeriodUuid,
        String academicPeriodCode,
        String academicPeriodName,

        Long intakeId,
        String intakeName,
        Long intakeSequenceNumber,

        BigDecimal total,

        List<Item> items,

        ChangeSummary changes
) {

    public record Item(
            Long ruleId,

            UUID feeItemUuid,
            String feeItemCode,
            String feeItemName,
            String feeItemCategory,

            BigDecimal amount,

            boolean inherited,
            Long sourceIntakeId,
            String sourceIntakeName
    ) {
    }

    public record ChangeSummary(
            int added,
            int changed,
            int removed,
            int unchanged
    ) {

        public static ChangeSummary empty() {
            return new ChangeSummary(0, 0, 0, 0);
        }
    }
}
