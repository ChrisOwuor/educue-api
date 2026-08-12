package com.owuor.educue.students.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentDetailResponse(
        UUID uuid, String status, LocalDate admissionDate,
        Long studentId, String admissionNumber, String studentName, String email, String phone,
        Long courseId, String courseCode, String courseName,
        Long intakeId, String intakeName,
        UUID enrolledAcademicYearUuid, String enrolledAcademicYearCode,
        UUID currentAcademicYearUuid, String currentAcademicYearCode,
        Long courseAcademicPeriodId, String academicPeriodCode, String academicPeriodName,
        Long departmentId, String departmentName
) {}
