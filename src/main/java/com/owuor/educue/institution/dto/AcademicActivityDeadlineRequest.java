package com.owuor.educue.institution.dto;

import com.owuor.educue.institution.enums.AcademicActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record AcademicActivityDeadlineRequest(
        @NotNull UUID academicYearUuid,
        @NotNull AcademicActivityType activityType,
        LocalDateTime startsAt,
        @NotNull LocalDateTime deadlineAt,
        @Size(max = 250) String description,
        Boolean active
) {}
