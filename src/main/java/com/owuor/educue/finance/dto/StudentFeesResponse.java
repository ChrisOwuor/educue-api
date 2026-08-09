package com.owuor.educue.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentFeesResponse(
        UUID courseAcademicPeriodUuid,
        String academicPeriodCode,
        String academicPeriodName,
        String intakeName,
        BigDecimal total,
        List<Item> items
) {

    public record Item(
            Long id,
            UUID feeItemUuid,
            String code,
            String name,
            BigDecimal amount
    ) {
    }
}
