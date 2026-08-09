package com.owuor.educue.mpesa.services;

import com.owuor.educue.mpesa.enums.MpesaEventStatus;
import com.owuor.educue.mpesa.repositories.MpesaPaymentEventRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class MpesaFailureService {
    private final MpesaPaymentEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long id, RuntimeException failure) {
        repository.findById(id).ifPresent(event -> {
            event.setProcessingStartedAt(null);
            event.setFailureDetail(limit(failure.getMessage()));
            if (failure instanceof MpesaEventProcessor.UnmatchedMpesaAccountException) {
                event.setStatus(MpesaEventStatus.REVIEW);
                event.setFailureCode("ACCOUNT_NOT_FOUND");
            } else if (event.getAttemptCount() >= 8) {
                event.setStatus(MpesaEventStatus.REVIEW);
                event.setFailureCode("RETRIES_EXHAUSTED");
            } else {
                event.setStatus(MpesaEventStatus.RETRY);
                event.setFailureCode("PROCESSING_ERROR");
                long delayMinutes = Math.min(60, 1L << Math.min(event.getAttemptCount(), 6));
                event.setNextAttemptAt(LocalDateTime.now().plusMinutes(delayMinutes));
            }
        });
    }
    private String limit(String value) {
        if (value == null) return "Unspecified processing error";
        return value.substring(0, Math.min(500, value.length()));
    }
}


