package com.owuor.educue.students.dto;

import java.util.List;

public record BulkPromotionRequest(
        List<Long> enrollmentIds
) {}
