package com.owuor.educue.institution.controller;

import com.owuor.educue.institution.dto.InstitutionProfileRequest;
import com.owuor.educue.institution.dto.InstitutionProfileResponse;
import com.owuor.educue.institution.service.InstitutionProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/institution-profile")
@RequiredArgsConstructor
public class InstitutionProfileController {

    private final InstitutionProfileService service;

    @GetMapping
    public InstitutionProfileResponse get() {
        return service.get();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public InstitutionProfileResponse update(@Valid @RequestBody InstitutionProfileRequest request) {
        return service.update(request);
    }
}
