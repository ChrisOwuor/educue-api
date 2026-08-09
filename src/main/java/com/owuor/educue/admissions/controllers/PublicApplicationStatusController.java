package com.owuor.educue.admissions.controllers;

import com.owuor.educue.admissions.dto.PublicApplicationStatusResponse;
import com.owuor.educue.admissions.service.AdmissionPackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/public/applications") @RequiredArgsConstructor
public class PublicApplicationStatusController {
    private final AdmissionPackService service;
    @GetMapping("/status") public List<PublicApplicationStatusResponse> status(@RequestParam String nationalId) {
        return service.lookup(nationalId);
    }
}
