package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.finance.dto.DebitStudentRequest;
import com.owuor.educue.finance.dto.ReverseLedgerEntryRequest;
import com.owuor.educue.users.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.owuor.educue.common.report.ProfessionalPdfService;

@RestController
@RequestMapping("/api/finance/ledger")
@RequiredArgsConstructor
public class FeeLedgerController {

    private final FeeLedgerService feeLedgerService;
    private final ProfessionalPdfService pdfService;

    /** Posts an auditable debit note for a resit, retake, penalty or other approved charge. */
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @PostMapping("/debits")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeLedgerResponse debitStudent(
            @Valid @RequestBody DebitStudentRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return feeLedgerService.debitStudent(request, currentUser.getId());
    }

    /** Creates an opposite credit/debit note and preserves the original entry for audit. */
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @PostMapping("/{ledgerId}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeLedgerResponse reverse(
            @PathVariable Long ledgerId,
            @Valid @RequestBody ReverseLedgerEntryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return feeLedgerService.reverse(ledgerId, request, currentUser.getId());
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

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAllFinancialData(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) java.util.UUID academicPeriodUuid,
            @RequestParam(required = false) com.owuor.educue.finance.enums.TransactionType transactionType,
            @RequestParam(required = false) java.time.LocalDate fromDate,
            @RequestParam(required = false) java.time.LocalDate toDate
    ) {
        var entries = feeLedgerService.getAllLedgerEntries(search, academicPeriodUuid, transactionType, fromDate, toDate);
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Ledger entries", String.valueOf(entries.size()));
        var rows = entries.stream().map(item -> java.util.List.of(
                item.getPostingDate().toString(), item.getDocumentNumber(), item.getAdmissionNumber(),
                item.getDescription(), item.getDebit().toString(), item.getCredit().toString(),
                item.getRunningBalance().toString())).toList();
        byte[] pdf = pdfService.tableReport("Financial Ledger Report", details,
                java.util.List.of("Date", "Document no.", "Reg. no.", "Description", "Debit", "Credit", "Balance"), rows,
                new float[]{1.15f, 1.25f, 1.30f, 2.85f, 1.15f, 1.15f, 1.10f});
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial-ledger.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
