package com.owuor.educue.mpesa.dtos;

import com.owuor.educue.mpesa.entities.MpesaPaymentEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MpesaEventResponse(Long id, String transactionId, LocalDateTime transactionTime,
        BigDecimal amount, String accountReference, String phoneLastFour, String status,
        int attemptCount, String receiptNumber, String failureCode, String failureDetail,
        LocalDateTime receivedAt, LocalDateTime processedAt) {
    public static MpesaEventResponse from(MpesaPaymentEvent e) {
        return new MpesaEventResponse(e.getId(), e.getTransactionId(), e.getTransactionTime(), e.getAmount(),
                e.getAccountReference(), e.getPhoneLastFour(), e.getStatus().name(), e.getAttemptCount(),
                e.getPayment() == null ? null : e.getPayment().getReceiptNumber(), e.getFailureCode(),
                e.getFailureDetail(), e.getReceivedAt(), e.getProcessedAt());
    }
}

