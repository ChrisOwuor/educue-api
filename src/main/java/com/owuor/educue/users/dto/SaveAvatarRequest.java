package com.owuor.educue.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveAvatarRequest(@NotBlank @Size(max = 1000) String secureUrl) { }
