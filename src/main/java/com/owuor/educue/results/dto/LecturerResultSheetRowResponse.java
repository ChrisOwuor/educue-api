package com.owuor.educue.results.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record LecturerResultSheetRowResponse(

        Long registrationId,

        Long studentId,
        String admissionNumber,
        String studentName,

        Long semesterUnitId,

        Long resultId,

        BigDecimal caMarks,
        BigDecimal examMarks,
        BigDecimal totalMarks,

        String grade,

        boolean passed,

        String status,

        String remarks

) {
}
