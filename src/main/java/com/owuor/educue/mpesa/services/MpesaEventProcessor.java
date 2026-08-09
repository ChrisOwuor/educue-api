package com.owuor.educue.mpesa.services;

import com.owuor.educue.mpesa.enums.MpesaEventStatus;
import com.owuor.educue.mpesa.entities.MpesaPaymentEvent;
import com.owuor.educue.mpesa.repositories.MpesaPaymentEventRepository;

import com.owuor.educue.finance.dto.RecordPaymentRequest;
import com.owuor.educue.finance.enums.PayerType;
import com.owuor.educue.finance.repository.PaymentRepository;
import com.owuor.educue.finance.service.PaymentService;
import com.owuor.educue.students.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class MpesaEventProcessor {
    private final MpesaPaymentEventRepository eventRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long eventId) {
        MpesaPaymentEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("M-PESA event not found"));
        if (event.getStatus() != MpesaEventStatus.PROCESSING) return;

        var existing = paymentRepository.findByGatewayReference(event.getTransactionId());
        if (existing.isPresent()) {
            complete(event, existing.get());
            return;
        }
        var student = studentRepository.findByAdmissionNumberIgnoreCase(event.getAccountReference())
                .orElseThrow(() -> new UnmatchedMpesaAccountException(event.getAccountReference()));

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setStudentId(student.getId());
        request.setAmount(event.getAmount());
        request.setPayerType(PayerType.STUDENT);
        request.setPayerName(student.getFullName());
        request.setGatewayReference(event.getTransactionId());
        request.setPaymentMethod("MPESA");
        request.setPaidAt(event.getTransactionTime());
        request.setRemarks("M-PESA C2B");
        var response = paymentService.recordProviderPayment(request);
        complete(event, paymentRepository.findById(response.getId()).orElseThrow());
    }

    private void complete(MpesaPaymentEvent event, com.owuor.educue.finance.entity.Payment payment) {
        event.setPayment(payment);
        event.setStatus(MpesaEventStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        event.setProcessingStartedAt(null);
        event.setFailureCode(null);
        event.setFailureDetail(null);
    }

    static class UnmatchedMpesaAccountException extends RuntimeException {
        UnmatchedMpesaAccountException(String reference) { super("No student matches account reference " + reference); }
    }
}


