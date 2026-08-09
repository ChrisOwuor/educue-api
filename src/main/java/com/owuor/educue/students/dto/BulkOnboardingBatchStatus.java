package com.owuor.educue.students.dto;

import java.util.UUID;

public record BulkOnboardingBatchStatus(
        UUID batchId,
        long total,
        long pending,
        long processing,
        long completed,
        long failed,
        long deadLetter
) {}
