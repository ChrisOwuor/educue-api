package com.owuor.educue.mpesa.services;

import com.owuor.educue.mpesa.entities.MpesaStkPushRequest;
import com.owuor.educue.mpesa.enums.StkPushStatus;
import com.owuor.educue.mpesa.dtos.StkPushResponse;
import com.owuor.educue.mpesa.dtos.InitiateStkPushRequest;
import com.owuor.educue.mpesa.repositories.MpesaStkPushRepository;

import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.students.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MpesaStkPushService {
    private final MpesaStkPushRepository repository;
    private final EnrollmentRepository enrollmentRepository;
    private final FeeLedgerService ledgerService;
    private final MpesaDarajaClient daraja;
    @Value("${app.mpesa.stk-short-code}")
    private String shortCode;
    @Value("${app.mpesa.stk-passkey}")
    private String passkey;
    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;
    @Value("${app.mpesa.stk-callback-token}")
    private String callbackToken;
    @Value("${app.mpesa.phone-hash-pepper}")
    private String phoneHashPepper;

    public StkPushResponse initiate(Long userId, InitiateStkPushRequest input) {
        var duplicate = repository.findByIdempotencyKey(input.idempotencyKey());
        if (duplicate.isPresent()) return StkPushResponse.from(duplicate.get());
        var enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student enrollment not found"));
        BigDecimal amount = input.amount().stripTrailingZeros();
        if (amount.scale() > 0)
            throw new IllegalArgumentException("M-PESA STK amount must be a whole Kenya shilling amount");
        BigDecimal balance = ledgerService.getStudentBalance(enrollment.getStudent().getId());
        if (balance.signum() <= 0)
            throw new IllegalArgumentException("This student account has no outstanding balance");
        if (amount.compareTo(balance) > 0)
            throw new IllegalArgumentException("Payment cannot exceed the current outstanding balance");
        String phone = normalizePhone(input.phoneNumber());
        MpesaStkPushRequest request = createRequest(enrollment, input.idempotencyKey(), amount, phone);
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString((shortCode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));
            var response = daraja.initiate(Map.ofEntries(
                    Map.entry("BusinessShortCode", shortCode), Map.entry("Password", password),
                    Map.entry("Timestamp", timestamp), Map.entry("TransactionType", "CustomerPayBillOnline"),
                    Map.entry("Amount", amount.toBigIntegerExact()), Map.entry("PartyA", phone),
                    Map.entry("PartyB", shortCode), Map.entry("PhoneNumber", phone),
                    Map.entry("CallBackURL", frontendBaseUrl + "/api/gateway/v1/transaction/" + callbackToken + "/notify"),
                    Map.entry("AccountReference", enrollment.getStudent().getAdmissionNumber()),
                    Map.entry("TransactionDesc", "Student fees")));
            return applyDarajaResponse(request.getId(), response);
        } catch (RuntimeException failure) {
            markInitiationFailed(request.getId(), failure.getMessage());
            throw new IllegalStateException("M-PESA could not accept the STK request. Please try again.");
        }
    }

    protected MpesaStkPushRequest createRequest(com.owuor.educue.students.entity.Enrollment enrollment,
                                                UUID idempotencyKey, BigDecimal amount, String phone) {
        MpesaStkPushRequest request = new MpesaStkPushRequest();
        request.setIdempotencyKey(idempotencyKey);
        request.setStudent(enrollment.getStudent());
        request.setCourseAcademicPeriod(enrollment.getCurrentCourseAcademicPeriod());
        request.setAccountReference(enrollment.getStudent().getAdmissionNumber());
        request.setAmount(amount);
        request.setPhoneHash(hash(phone));
        request.setPhoneLastFour(phone.substring(phone.length() - 4));
        return repository.saveAndFlush(request);
    }

    protected StkPushResponse applyDarajaResponse(Long id, MpesaDarajaClient.StkApiResponse response) {
        var request = repository.findById(id).orElseThrow();
        if (response == null || response.checkoutRequestId() == null)
            throw new IllegalStateException("Invalid Daraja response");
        request.setMerchantRequestId(response.merchantRequestId());
        request.setCheckoutRequestId(response.checkoutRequestId());
        request.setResponseCode(response.responseCode());
        request.setResponseDescription(response.responseDescription());
        request.setCustomerMessage(response.customerMessage());
        request.setStatus("0".equals(response.responseCode()) ? StkPushStatus.PENDING : StkPushStatus.FAILED);
        return StkPushResponse.from(repository.save(request));
    }

    protected void markInitiationFailed(Long id, String detail) {
        repository.findById(id).ifPresent(r -> {
            r.setStatus(StkPushStatus.FAILED);
            r.setResultDescription(limit(detail));
            repository.save(r);
        });
    }

    @Transactional(readOnly = true)
    public StkPushResponse status(Long userId, UUID requestId) {
        return repository.findByUuidAndStudentUserId(requestId, userId).map(StkPushResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("STK request not found"));
    }

    private String normalizePhone(String raw) {
        String phone = raw.replaceAll("\\D", "");
        if (phone.startsWith("0") && phone.length() == 10) phone = "254" + phone.substring(1);
        else if (phone.startsWith("1") && phone.length() == 9) phone = "254" + phone;
        else if (phone.startsWith("7") && phone.length() == 9) phone = "254" + phone;
        if (!phone.matches("254[17]\\d{8}"))
            throw new IllegalArgumentException("Enter a valid Kenyan M-PESA phone number");
        return phone;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((value + phoneHashPepper).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String limit(String value) {
        return value == null ? "Daraja initiation failed" : value.substring(0, Math.min(500, value.length()));
    }
}
