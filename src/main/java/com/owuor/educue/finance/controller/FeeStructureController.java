package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.CreateFeeStructureRequest;
import com.owuor.educue.finance.dto.FeeStructureResponse;
import com.owuor.educue.finance.service.FeeStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.owuor.educue.common.report.ProfessionalPdfService;

import java.util.List;

@RestController
@RequestMapping("/api/finance/fee-structures")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;
    private final ProfessionalPdfService pdfService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeeStructureResponse create(
            @Valid @RequestBody CreateFeeStructureRequest request
    ) {
        return feeStructureService.create(request);
    }

    @GetMapping("/{id}")
    public FeeStructureResponse get(
            @PathVariable Long id
    ) {
        return feeStructureService.get(id);
    }

    @GetMapping("/student/{studentUserId}")
    public FeeStructureResponse getStudentFeeStructure(
            @PathVariable Long studentUserId
    ) {
        return feeStructureService.getStudentFeeStructure(studentUserId);
    }

    @GetMapping
    public List<FeeStructureResponse> getAll() {
        return feeStructureService.getAll();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        FeeStructureResponse structure = feeStructureService.get(id);
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Course", structure.getCourseName());
        details.put("Intake", structure.getIntakeName());
        details.put("Academic period", structure.getAcademicPeriodName());
        details.put("Total", "KES " + structure.getTotal());
        var rows = structure.getItems().stream()
                .map(item -> java.util.List.of(item.getName(), "KES " + item.getAmount()))
                .toList();
        byte[] pdf = pdfService.tableReport("Fee Structure", details, java.util.List.of("Fee item", "Amount"), rows);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=fee-structure-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {
        feeStructureService.delete(id);
    }
}
