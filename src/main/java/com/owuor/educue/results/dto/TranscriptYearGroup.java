package com.owuor.educue.results.dto;

import com.owuor.educue.results.dto.StudentResultResponse;

import java.math.BigDecimal;
import java.util.List;

public record TranscriptYearGroup(
        Integer yearNumber,
        String yearTitle,

        BigDecimal currentAverage,
        BigDecimal cumulativeAverage,

        String recommendation,

        List<StudentResultResponse> units
) {
}
