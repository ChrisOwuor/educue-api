package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.CreateFeeStructureRequest;
import com.owuor.educue.finance.dto.FeeStructureResponse;
import com.owuor.educue.finance.service.FeeStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/fee-structures")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {
        feeStructureService.delete(id);
    }
}
