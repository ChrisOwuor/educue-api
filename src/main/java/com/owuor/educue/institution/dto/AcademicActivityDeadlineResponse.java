package com.owuor.educue.institution.dto;

import com.owuor.educue.institution.entity.AcademicActivityDeadline;
import com.owuor.educue.institution.enums.AcademicActivityType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AcademicActivityDeadlineResponse(UUID uuid, UUID academicYearUuid, String academicYearCode,
        AcademicActivityType activityType, LocalDateTime startsAt, LocalDateTime deadlineAt, String description, boolean active,
        boolean passed) {
    public static AcademicActivityDeadlineResponse from(AcademicActivityDeadline value) {
        return new AcademicActivityDeadlineResponse(value.getUuid(), value.getAcademicYear().getUuid(),
                value.getAcademicYear().getCode(), value.getActivityType(), value.getStartsAt(), value.getDeadlineAt(),
                value.getDescription(), value.isActive(), value.isActive() && LocalDateTime.now().isAfter(value.getDeadlineAt()));
    }
}
