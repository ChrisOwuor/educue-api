package com.owuor.educue.admissions.dto;

import java.util.UUID;

public record CourseIntakeConfigurationResponse(
        Long intakeId, String intakeName,
        Long courseId, UUID courseUuid, String courseCode, String courseName,
        UUID firstPeriodUuid, String firstPeriodCode,
        long configuredUnitCount, long configuredFeeCount,
        boolean unitsConfigured, boolean feesConfigured,
        boolean academicConfirmed, boolean feeConfirmed,
        boolean addedToIntake, boolean ready
) {}
