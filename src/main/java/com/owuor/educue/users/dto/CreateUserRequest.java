package com.owuor.educue.users.dto;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String phone,
        @NotNull Long roleId,
        Long departmentId, // optional - ADMIN doesn't need one, HOD/TRAINER typically do
        @NotBlank String password
) {}
