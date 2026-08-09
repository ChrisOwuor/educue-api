package com.owuor.educue.mpesa.services;

import com.owuor.educue.mpesa.dtos.MpesaConfirmationRequest;
import com.owuor.educue.mpesa.repositories.MpesaPaymentEventRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Service @RequiredArgsConstructor
public class MpesaWebhookService {
    private static final DateTimeFormatter MPESA_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final MpesaPaymentEventRepository repository;
    @Value("${app.mpesa.c2b-short-code}") private String expectedShortCode;
    @Value("${app.mpesa.phone-hash-pepper}") private String phoneHashPepper;

    @Transactional
    public void receive(MpesaConfirmationRequest request) {
        validate(request);
        String phoneHash = null;
        String phoneLastFour = null;
        if (request.msisdn() != null && !request.msisdn().isBlank()) {
            String normalized = request.msisdn().replaceAll("\\D", "");
            phoneHash = sha256(normalized + phoneHashPepper);
            phoneLastFour = normalized.substring(Math.max(0, normalized.length() - 4));
        }
        repository.insertIfAbsent(request.transactionId().trim().toUpperCase(), clean(request.transactionType()),
                LocalDateTime.parse(request.transactionTime(), MPESA_TIME), new BigDecimal(request.amount()),
                request.businessShortCode().trim(), request.accountReference().trim().toUpperCase(),
                clean(request.invoiceNumber()), clean(request.thirdPartyTransactionId()), phoneHash, phoneLastFour);
    }

    private void validate(MpesaConfirmationRequest r) {
        if (blank(r.transactionId()) || blank(r.transactionTime()) || blank(r.amount())
                || blank(r.businessShortCode()) || blank(r.accountReference()))
            throw new IllegalArgumentException("Missing required M-PESA confirmation fields");
        if (!expectedShortCode.equals(r.businessShortCode().trim()))
            throw new IllegalArgumentException("Invalid business short code");
        BigDecimal amount;
        try { amount = new BigDecimal(r.amount()); LocalDateTime.parse(r.transactionTime(), MPESA_TIME); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid M-PESA amount or transaction time"); }
        if (amount.signum() <= 0) throw new IllegalArgumentException("M-PESA amount must be positive");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String clean(String value) { return blank(value) ? null : value.trim(); }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }
}

