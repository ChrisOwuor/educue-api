package com.owuor.educue.admissions.dto;

import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.entity.IntakeCourse;
import com.owuor.educue.admissions.enums.IntakeStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record IntakeResponse(
        Long id,
        UUID uuid,
        String name,
        UUID academicYearUuid,
        Long academicYearId,
        String academicYearCode,
        LocalDate startDate,
        LocalDate applicationDeadline,
        IntakeStatus status,
        Long sequenceNumber,
        List<CourseSummary> courses
) {
    public static IntakeResponse from(Intake intake, List<IntakeCourse> intakeCourses) {
        return new IntakeResponse(
                intake.getId(),
                intake.getUuid(),
                intake.getName(),
                intake.getAcademicYear().getUuid(),
                intake.getAcademicYear().getId(),
                intake.getAcademicYear().getCode(),
                intake.getStartDate(),
                intake.getApplicationDeadline(),
                intake.getStatus(),
                intake.getSequenceNumber(),
                intakeCourses.stream()
                        .map(CourseSummary::from)
                        .toList()
        );
    }
}
