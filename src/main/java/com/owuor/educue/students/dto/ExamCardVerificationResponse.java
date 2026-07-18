package com.owuor.educue.students.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExamCardVerificationResponse(
        boolean valid,
        UUID verificationCode,
        String admissionNumber,
        String studentName,
        String course,
        String academicPeriod,
        LocalDateTime issuedAt,
        List<ExamUnit> authorizedExams
) {
    public record ExamUnit(String code, String name) {}
}
