package com.owuor.educue.finance.mpesa;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/finance/mpesa/events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
public class MpesaOperationsController {
    private final MpesaPaymentEventRepository repository;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<MpesaEventResponse> events(Pageable pageable) {
        return repository.findAll(pageable).map(MpesaEventResponse::from);
    }

    @PostMapping("/{id}/retry")
    @Transactional
    public MpesaEventResponse retry(@PathVariable Long id) {
        var event = repository.findById(id).orElseThrow();
        if (event.getStatus() == MpesaEventStatus.PROCESSED)
            throw new IllegalArgumentException("A processed M-PESA event cannot be retried");
        event.setStatus(MpesaEventStatus.RETRY);
        event.setNextAttemptAt(LocalDateTime.now());
        event.setFailureCode(null);
        event.setFailureDetail(null);
        return MpesaEventResponse.from(event);
    }
}
