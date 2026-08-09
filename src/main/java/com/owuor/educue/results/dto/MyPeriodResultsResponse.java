package com.owuor.educue.results.dto;

import java.math.BigDecimal;
import java.util.List;

public record MyPeriodResultsResponse(
        PeriodSummary summary,
        List<StudentResultResponse> results
) {

    public record PeriodSummary(
            Long courseAcademicPeriodId,
            String academicPeriodCode,
            String academicPeriodName,

            BigDecimal average,
            BigDecimal totalWeightedMarks,

            int totalUnits,
            int totalCredits,
            int passedUnits,
            int failedUnits
    ) {
    }
}
