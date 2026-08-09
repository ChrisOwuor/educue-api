package com.owuor.educue.graduation.controller;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.graduation.dto.GraduationFeeStructureResponse;
import com.owuor.educue.graduation.dto.SaveGraduationFeeStructureRequest;
import com.owuor.educue.graduation.service.GraduationFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graduation-fees")
@RequiredArgsConstructor
public class GraduationFeeController {
    private final GraduationFeeService service;

    @GetMapping("/structure")
    public GraduationFeeStructureResponse get(@RequestParam QualificationType qualificationType, @RequestParam Long intakeId) {
        return service.getStructure(qualificationType, intakeId);
    }

    @PutMapping("/structure")
    public GraduationFeeStructureResponse save(@Valid @RequestBody SaveGraduationFeeStructureRequest request) {
        return service.saveStructure(request);
    }
}
