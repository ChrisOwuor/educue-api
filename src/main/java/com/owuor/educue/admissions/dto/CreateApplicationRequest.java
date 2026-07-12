package com.owuor.educue.admissions.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotNull Long intakeId,
        @NotNull Long courseId,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        String nationalId,
        LocalDate dateOfBirth,
        String guardianName,
        String guardianPhone
) {}
