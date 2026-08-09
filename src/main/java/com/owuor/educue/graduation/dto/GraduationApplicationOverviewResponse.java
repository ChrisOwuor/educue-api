package com.owuor.educue.graduation.dto;

import java.time.LocalDateTime;

public record GraduationApplicationOverviewResponse(
        Long applicationId,
        String admissionNumber,
        String studentName,
        String courseCode,
        String courseName,
        String status,
        LocalDateTime appliedAt
) {
}
