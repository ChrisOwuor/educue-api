package com.owuor.educue.institution.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionProfileRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 30) String shortName,
        @Size(max = 80) String registrationNumber,
        @Size(max = 180) String motto,
        @Email @Size(max = 150) String officialEmail,
        @Size(max = 30) String phone,
        @Size(max = 255) String website,
        @Size(max = 300) String address
) {
}
