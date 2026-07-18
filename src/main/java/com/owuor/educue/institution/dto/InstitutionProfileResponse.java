package com.owuor.educue.institution.dto;

import com.owuor.educue.institution.entity.InstitutionProfile;

import java.time.LocalDateTime;
import java.util.UUID;

public record InstitutionProfileResponse(
        UUID uuid,
        String name,
        String shortName,
        String registrationNumber,
        String motto,
        String officialEmail,
        String phone,
        String website,
        String address,
        Long version,
        LocalDateTime updatedAt
) {
    public static InstitutionProfileResponse from(InstitutionProfile profile) {
        return new InstitutionProfileResponse(
                profile.getUuid(), profile.getName(), profile.getShortName(),
                profile.getRegistrationNumber(), profile.getMotto(), profile.getOfficialEmail(),
                profile.getPhone(), profile.getWebsite(), profile.getAddress(),
                profile.getVersion(), profile.getUpdatedAt()
        );
    }
}
