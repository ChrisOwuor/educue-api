package com.owuor.educue.students.dto;

import lombok.Builder;

@Builder
public record EnrollmentResponse(

        Long id,

        Long studentId,

        String admissionNumber,

        String studentName,

        String email,

        String courseName,

        Long courseAcademicPeriodId,

        String academicPeriodName,

        String status,
        Long studentUserId

) {
}
