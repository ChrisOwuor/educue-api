package com.owuor.educue.finance.mpesa;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
public record StkPushResponse(UUID requestId, String status, BigDecimal amount, String phoneLastFour,
                              String customerMessage, String resultDescription, LocalDateTime requestedAt) {
    static StkPushResponse from(MpesaStkPushRequest r) {
        return new StkPushResponse(r.getUuid(), r.getStatus().name(), r.getAmount(), r.getPhoneLastFour(),
                r.getCustomerMessage(), r.getResultDescription(), r.getRequestedAt());
    }
}
