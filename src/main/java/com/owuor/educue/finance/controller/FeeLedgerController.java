package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.ChargeStudentRequest;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.service.FeeLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/finance/ledger")
@RequiredArgsConstructor
public class FeeLedgerController {

    private final FeeLedgerService feeLedgerService;

    /**
     * Charge a student based on a fee structure.
     * Creates a TUITION_BILL entry in their fee ledger.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeLedgerResponse chargeStudent(
            @Valid @RequestBody ChargeStudentRequest request
    ) {
        return feeLedgerService.chargeStudent(request);
    }

    /**
     * Get all ledger entries for a specific student (their full statement).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @GetMapping("/student/{studentId}")
    public List<FeeLedgerResponse> getStudentLedger(
            @PathVariable Long studentId
    ) {
        return feeLedgerService.getStudentLedger(studentId);
    }

    /**
     * Get the current balance for a specific student.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @GetMapping("/student/{studentId}/balance")
    public BigDecimal getStudentBalance(
            @PathVariable Long studentId
    ) {
        return feeLedgerService.getStudentBalance(studentId);
    }
}
