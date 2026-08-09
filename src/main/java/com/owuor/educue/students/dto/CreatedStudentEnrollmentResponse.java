package com.owuor.educue.students.dto;

public record CreatedStudentEnrollmentResponse(
        Long studentId, Long enrollmentId, Long userId,
        String admissionNumber, String fullName, String temporaryPassword
) {}
