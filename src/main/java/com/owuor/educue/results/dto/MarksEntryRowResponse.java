package com.owuor.educue.results.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MarksEntryRowResponse(

        Long registrationId,

        Long studentId,

        String admissionNumber,

        String studentName,

        Long courseUnitPlacementId,

        String unitCode,

        String unitName,

        Long courseId,
        String courseCode,
        String courseName,

        String attemptType,

        Long resultId,

        BigDecimal caMarks,

        BigDecimal examMarks,

        BigDecimal totalMarks,

        String grade,

        String status
) {
}
