package com.owuor.educue.mpesa.controllers;

import com.owuor.educue.mpesa.dtos.C2bUrlRegistrationResponse;
import com.owuor.educue.mpesa.services.MpesaC2bRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/mpesa/configuration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MpesaConfigurationController {
    private final MpesaC2bRegistrationService registrationService;

    @PostMapping("/register-urls")
    public C2bUrlRegistrationResponse registerUrls() {
        return registrationService.register();
    }
}
