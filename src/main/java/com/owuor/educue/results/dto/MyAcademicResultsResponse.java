package com.owuor.educue.results.dto;

import java.math.BigDecimal;
import java.util.List;

public record MyAcademicResultsResponse(
        List<AcademicPeriodResult> periods
) {

    public record AcademicPeriodResult(
            Long courseAcademicPeriodId,

            String academicPeriodCode,
            String academicPeriodName,

            Integer yearNumber,
            Integer periodNumber,
            Integer position,

            BigDecimal currentAverage,
            BigDecimal cumulativeAverage,

            String recommendation,

            List<StudentResultResponse> units
    ) {
    }
}
