package com.owuor.educue.students.dto;

import java.util.UUID;

public record BulkOnboardingBatchResponse(UUID batchId, int rowCount, String status) {}
