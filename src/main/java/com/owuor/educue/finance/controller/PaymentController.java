package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.PaymentResponse;
import com.owuor.educue.finance.dto.RecordPaymentRequest;
import com.owuor.educue.finance.service.PaymentService;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse recordPayment(
            @Valid @RequestBody RecordPaymentRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return paymentService.recordPayment(request, currentUser.getId());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @GetMapping
    public Page<PaymentResponse> searchPayments(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return paymentService.searchPayments(studentId, search, page, size);
    }
}
