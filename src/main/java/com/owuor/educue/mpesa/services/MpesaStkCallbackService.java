package com.owuor.educue.mpesa.services;

import com.owuor.educue.mpesa.entities.MpesaStkPushRequest;
import com.owuor.educue.mpesa.enums.StkPushStatus;
import com.owuor.educue.mpesa.repositories.MpesaStkPushRepository;
import com.owuor.educue.mpesa.repositories.MpesaPaymentEventRepository;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;

@Service @RequiredArgsConstructor
public class MpesaStkCallbackService {
    private final MpesaStkPushRepository stkRepository;
    private final MpesaPaymentEventRepository eventRepository;
    @Value("${app.mpesa.stk-short-code}") private String shortCode;

    @Transactional
    public void receive(JsonNode payload) {
        JsonNode callback = payload.path("Body").path("stkCallback");
        String checkoutId = requiredText(callback, "CheckoutRequestID");
        MpesaStkPushRequest request = stkRepository.findByCheckoutRequestId(checkoutId)
                .orElseThrow(() -> new EntityNotFoundException("Unknown M-PESA checkout request"));
        // A late successful callback is authoritative even if initiation timed out locally.
        if (request.getStatus() == StkPushStatus.SUCCESS) return;
        int resultCode = callback.path("ResultCode").asInt(-1);
        request.setResultCode(resultCode);
        request.setResultDescription(callback.path("ResultDesc").asText("M-PESA request completed"));
        request.setCallbackReceivedAt(LocalDateTime.now());
        if (resultCode != 0) {
            request.setStatus(StkPushStatus.FAILED);
            return;
        }
        JsonNode items = callback.path("CallbackMetadata").path("Item");
        String receipt = metadata(items, "MpesaReceiptNumber");
        String amountText = metadata(items, "Amount");
        String transactionDate = metadata(items, "TransactionDate");
        if (receipt == null || amountText == null || transactionDate == null)
            throw new IllegalArgumentException("Successful STK callback is missing settlement metadata");
        BigDecimal settledAmount = new BigDecimal(amountText);
        if (settledAmount.compareTo(request.getAmount()) != 0)
            throw new IllegalArgumentException("STK settlement amount does not match the initiated amount");
        request.setMpesaReceiptNumber(receipt.toUpperCase());
        request.setStatus(StkPushStatus.SUCCESS);
        eventRepository.insertIfAbsent(receipt.toUpperCase(), "STK Push", parseDate(transactionDate), settledAmount,
                shortCode, request.getAccountReference(),
                request.getUuid().toString(), checkoutId, request.getPhoneHash(), request.getPhoneLastFour());
    }

    private LocalDateTime parseDate(String value) {
        return LocalDateTime.parse(value.replaceAll("\\.0$", ""), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
    private String requiredText(JsonNode node, String name) {
        String value = node.path(name).asText(null);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }
    private String metadata(JsonNode items, String name) {
        if (!items.isArray()) return null;
        for (JsonNode item : items) if (name.equals(item.path("Name").asText())) return item.path("Value").asText(null);
        return null;
    }
}

