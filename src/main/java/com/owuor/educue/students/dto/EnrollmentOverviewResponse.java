package com.owuor.educue.students.dto;

import com.owuor.educue.students.enums.EnrollmentStatus;
import java.time.LocalDate;
import java.util.UUID;

/** Minimal enrollment list row; personal student details are deliberately excluded. */
public record EnrollmentOverviewResponse(
        UUID uuid,
        String admissionNumber,
        String academicPeriodCode,
        String academicPeriodName,
        String courseCode,
        String courseName,
        String status,
        LocalDate admissionDate
) {
    public EnrollmentOverviewResponse(UUID uuid, String admissionNumber,
                                      String academicPeriodCode, String academicPeriodName,
                                      String courseCode, String courseName,
                                      EnrollmentStatus status, LocalDate admissionDate) {
        this(uuid, admissionNumber, academicPeriodCode, academicPeriodName,
                courseCode, courseName, status.name(), admissionDate);
    }
}
