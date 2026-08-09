package com.owuor.educue.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EffectivePeriodFeesResponse(
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

        BigDecimal mandatoryTotal,
        BigDecimal optionalTotal,
        BigDecimal grandTotal,

        List<PeriodFeeItemResponse> items
) {
}
