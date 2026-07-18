package com.owuor.educue.finance.mpesa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j @Component @RequiredArgsConstructor
public class MpesaInboxWorker {
    private final MpesaPaymentEventRepository repository;
    private final MpesaClaimService claimService;
    private final MpesaEventProcessor processor;
    private final MpesaFailureService failureService;

    @Scheduled(fixedDelayString = "${app.mpesa.worker-delay-ms:500}")
    public void drain() {
        for (Long id : claimService.claim(100)) {
            try { processor.process(id); }
            catch (RuntimeException failure) {
                log.warn("M-PESA event {} processing failed: {}", id, failure.getMessage());
                failureService.record(id, failure);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.mpesa.recovery-delay-ms:60000}")
    public void recoverAbandonedClaims() {
        claimService.recoverStale();
    }
}
