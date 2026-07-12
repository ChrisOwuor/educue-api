package com.owuor.educue.students.dto;

public record PromotionResultResponse(
        Long enrollmentId,
        String studentName,
        String admissionNumber,
        boolean promoted,
        String reason // e.g., "Promoted to Year 1 Semester 2", "Failed mandatory unit: ICT101"
) {}
