package com.owuor.educue.academics.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/** One placed unit and its optional current trainer allocation. */
@Builder
public record CourseUnitAllocationResponse(
        UUID courseUnitPlacementUuid,
        Long assignmentId,
        Long lecturerId,
        String lecturerName,
        Long unitId,
        String unitCode,
        String unitName,
        Long courseAcademicPeriodId,
        UUID courseAcademicPeriodUuid,
        String academicPeriodName,
        Long courseId,
        UUID courseUuid,
        String courseName,
        UUID effectiveFromAcademicYearUuid,
        String effectiveFromAcademicYearCode,
        UUID effectiveToAcademicYearUuid,
        String effectiveToAcademicYearCode,
        LocalDateTime assignedAt,
        Long assignedById,
        String assignedByName
) {
    public boolean assigned() {
        return lecturerId != null;
    }
}
