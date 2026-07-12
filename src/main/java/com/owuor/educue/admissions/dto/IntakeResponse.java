package com.owuor.educue.admissions.dto;

import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.entity.IntakeCourse;
import com.owuor.educue.admissions.enums.IntakeStatus;

import java.time.LocalDate;
import java.util.List;

public record IntakeResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate applicationDeadline,
        IntakeStatus status,
        List<CourseSummary> courses
) {
    public static IntakeResponse from(Intake intake, List<IntakeCourse> intakeCourses) {
        return new IntakeResponse(
                intake.getId(),
                intake.getName(),
                intake.getStartDate(),
                intake.getApplicationDeadline(),
                intake.getStatus(),
                intakeCourses.stream()
                        .map(ic -> CourseSummary.from(ic.getCourse()))
                        .toList()
        );
    }
}
