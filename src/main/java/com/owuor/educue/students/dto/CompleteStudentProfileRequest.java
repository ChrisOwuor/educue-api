package com.owuor.educue.students.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CompleteStudentProfileRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 30) String nationalId,
        @NotNull LocalDate dateOfBirth,
        @NotBlank @Size(max = 150) String guardianName,
        @NotBlank @Size(max = 20) String guardianPhone
) {}
