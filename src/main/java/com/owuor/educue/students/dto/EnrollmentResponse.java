package com.owuor.educue.students.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record EnrollmentResponse(

        Long id,

        Long studentId,

        String admissionNumber,

        String studentName,

        String email,

        String courseName,

        Long courseId,

        Long intakeId,

        String intakeName,

        UUID enrolledAcademicYearUuid,

        String enrolledAcademicYearCode,

        UUID currentAcademicYearUuid,

        String currentAcademicYearCode,

        Long courseAcademicPeriodId,

        String academicPeriodName,

        String status,
        Long studentUserId

) {
}
