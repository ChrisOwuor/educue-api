package com.owuor.educue.results.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record StudentResultResponse(
        Long resultId,
        Long studentId,
        String studentName,
        String admissionNumber,
        String unitCode,
        String unitName,
        String course,
        String academicPeriod,
        String attemptType,
        BigDecimal caMarks,
        BigDecimal examMarks,
        BigDecimal totalMarks,
        String grade,
        boolean passed,
        String status,
        String remarks,
        String recordedByName,
        String approvedByName,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
