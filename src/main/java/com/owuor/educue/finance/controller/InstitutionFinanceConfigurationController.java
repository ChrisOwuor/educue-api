package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.InstitutionFinanceConfigurationResponse;
import com.owuor.educue.finance.service.InstitutionFinanceConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/institution-configuration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InstitutionFinanceConfigurationController {
    private final InstitutionFinanceConfigurationService service;

    @GetMapping
    public InstitutionFinanceConfigurationResponse get() {
        return service.get();
    }

    @PutMapping
    public InstitutionFinanceConfigurationResponse save(@RequestBody InstitutionFinanceConfigurationResponse request) {
        return service.save(request);
    }
}
