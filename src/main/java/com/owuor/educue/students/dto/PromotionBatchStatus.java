package com.owuor.educue.students.dto;

import java.util.UUID;

public record PromotionBatchStatus(
        UUID batchId,
        long total,
        long pending,
        long processing,
        long completed,
        long skipped,
        long failed,
        long deadLetter
) {}
