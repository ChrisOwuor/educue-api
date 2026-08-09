package com.owuor.educue.students.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateStudentEnrollmentRequest(
        @NotBlank
        @Size(max = 30)
        String admissionNumber,

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @Size(max = 20)
        String phone,

        @Size(max = 30)
        String nationalId,

        LocalDate dateOfBirth,

        @Size(max = 150)
        String guardianName,

        @Size(max = 20)
        String guardianPhone,

        @NotNull
        Long courseId,

        @NotNull
        Long intakeId,

        @NotNull
        UUID enrolledAcademicYearUuid,

        @NotNull
        UUID currentAcademicYearUuid,

        @NotNull
        UUID currentCourseAcademicPeriodUuid,

        @NotNull
        LocalDate admissionDate,

        Boolean migrated,

        @PositiveOrZero
        BigDecimal openingDebit,

        @PositiveOrZero
        BigDecimal openingCredit,

        LocalDate openingBalanceDate,

        @Size(max = 100)
        String legacyReference
) {
}
