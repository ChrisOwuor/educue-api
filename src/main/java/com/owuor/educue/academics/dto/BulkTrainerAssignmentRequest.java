package com.owuor.educue.academics.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BulkTrainerAssignmentRequest(@NotNull Long lecturerId,
                                           @NotEmpty List<UUID> courseUnitPlacementUuids,
                                           @NotNull UUID effectiveFromAcademicYearUuid,
                                           UUID effectiveToAcademicYearUuid) { }
