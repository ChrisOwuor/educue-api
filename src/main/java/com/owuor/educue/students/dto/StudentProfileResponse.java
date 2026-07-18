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

        Integer currentYear,

        java.util.UUID courseAcademicPeriodUuid,

        String academicPeriodCode,

        String academicPeriodName,

        String academicPeriodType,

        Integer academicPeriodNumber,

        String enrollmentStatus,

        LocalDate admissionDate

) {
}
