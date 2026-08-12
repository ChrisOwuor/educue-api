package com.owuor.educue.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(max = 80) String username,
        @NotNull Long departmentId,
        Boolean active,
        @Size(min = 8, max = 100) String newPassword
) {}
