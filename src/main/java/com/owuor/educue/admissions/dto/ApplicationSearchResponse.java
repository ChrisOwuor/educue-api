package com.owuor.educue.admissions.dto;

import java.util.List;
import java.util.Map;

public record ApplicationSearchResponse(
        List<ApplicationResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        Map<String, Long> statusCounts
) {}
