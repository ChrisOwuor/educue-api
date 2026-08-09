package com.owuor.educue.admissions.dto;

import java.util.List;

public record IntakeDetailResponse(
        IntakeResponse intake,
        List<CourseIntakeConfigurationResponse> courseConfigurations,
        boolean publishable
) {}
