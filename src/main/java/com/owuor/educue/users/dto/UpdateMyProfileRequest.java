package com.owuor.educue.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record UpdateMyProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 80) String username) { }
