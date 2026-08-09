package com.owuor.educue.students.dto;

import java.util.UUID;

public record PromotionBatchResponse(UUID batchId, int enrollmentCount, String status) {}
