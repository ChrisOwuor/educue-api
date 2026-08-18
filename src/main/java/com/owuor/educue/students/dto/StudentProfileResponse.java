package com.owuor.educue.students.dto;

import lombok.Builder;

import java.time.LocalDate;


@Builder
public record StudentProfileResponse(

        Long studentId,

        String admissionNumber,

        String fullName,

        String email,

        String phone,

        String courseCode,

        String courseName,

        Long intakeId,

        String intakeName,

        java.util.UUID enrolledAcademicYearUuid,

        String enrolledAcademicYearCode,

        java.util.UUID currentAcademicYearUuid,

        String currentAcademicYearCode,

        Integer currentYear,

        java.util.UUID courseAcademicPeriodUuid,

        String academicPeriodCode,

        String academicPeriodName,

        String academicPeriodType,

        Integer academicPeriodNumber,

        String enrollmentStatus,

        LocalDate admissionDate,

        boolean profileCompletionRequired

) {
}
