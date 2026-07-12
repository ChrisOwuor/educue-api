package com.owuor.educue.students.dto;

public record PromotionStats(
        Long enrollmentId,
        long requiredMandatory,
        long passedMandatory,
        long totalRegistrations
) {}
