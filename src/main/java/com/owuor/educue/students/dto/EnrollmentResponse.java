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

        String curriculumName,

        String semesterName,

        String status,
        Long studentUserId,
        Long appliedSemesterId

) {
}
