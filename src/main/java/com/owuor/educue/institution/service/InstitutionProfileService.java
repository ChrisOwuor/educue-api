package com.owuor.educue.institution.service;

import com.owuor.educue.institution.dto.InstitutionProfileRequest;
import com.owuor.educue.institution.dto.InstitutionProfileResponse;
import com.owuor.educue.institution.entity.InstitutionProfile;
import com.owuor.educue.institution.repository.InstitutionProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstitutionProfileService {

    private static final long PROFILE_ID = 1L;
    private final InstitutionProfileRepository repository;

    @Transactional(readOnly = true)
    public InstitutionProfileResponse get() {
        return InstitutionProfileResponse.from(findProfile());
    }

    @Transactional
    public InstitutionProfileResponse update(InstitutionProfileRequest request) {
        InstitutionProfile profile = findProfile();
        profile.setName(request.name().trim());
        profile.setShortName(clean(request.shortName()));
        profile.setRegistrationNumber(clean(request.registrationNumber()));
        profile.setMotto(clean(request.motto()));
        profile.setOfficialEmail(clean(request.officialEmail()));
        profile.setPhone(clean(request.phone()));
        profile.setWebsite(clean(request.website()));
        profile.setAddress(clean(request.address()));
        return InstitutionProfileResponse.from(repository.save(profile));
    }

    private InstitutionProfile findProfile() {
        return repository.findById(PROFILE_ID)
                .orElseThrow(() -> new EntityNotFoundException("Institution profile is not initialized"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
