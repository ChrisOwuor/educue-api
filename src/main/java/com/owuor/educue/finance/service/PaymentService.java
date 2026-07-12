package com.owuor.educue.finance.service;

import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.repository.SemesterRepository;
import com.owuor.educue.finance.dto.PaymentResponse;
import com.owuor.educue.finance.dto.RecordPaymentRequest;
import com.owuor.educue.finance.entity.Payment;
import com.owuor.educue.finance.enums.PaymentStatus;
import com.owuor.educue.finance.enums.TransactionType;
import com.owuor.educue.finance.repository.PaymentRepository;
import com.owuor.educue.students.entity.Student;
import com.owuor.educue.students.repository.StudentRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final FeeLedgerService feeLedgerService;

    /**
     * Records a new payment, marks it as VERIFIED, and instantly credits the student's ledger.
     */
    public PaymentResponse recordPayment(RecordPaymentRequest request, Long recordedById) {

        if (paymentRepository.existsByGatewayReference(request.getGatewayReference())) {
            throw new IllegalArgumentException("A payment with this gateway reference already exists.");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        Semester appliedSemester = null;
        if (request.getAppliedSemesterId() != null) {
            appliedSemester = semesterRepository.findById(request.getAppliedSemesterId())
                    .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        }

        User recordedBy = userRepository.findById(recordedById)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Generate unique internal receipt number
        String receiptNumber = "RCPT-" + System.currentTimeMillis();

        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setAppliedSemester(appliedSemester);
        payment.setAmount(request.getAmount());
        payment.setGatewayReference(request.getGatewayReference());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaidAt(request.getPaidAt());
        payment.setReceiptNumber(receiptNumber);
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setRecordedBy(recordedBy);
        payment.setRemarks(request.getRemarks());

        payment = paymentRepository.save(payment);

        // Instantly reconcile the ledger
        TransactionType txType = request.getPaymentMethod().equalsIgnoreCase("MPESA")
                ? TransactionType.PAYMENT_MPESA
                : TransactionType.PAYMENT_BANK;
                
        String description = String.format("Payment via %s (Ref: %s)", 
                request.getPaymentMethod(), request.getGatewayReference());

        feeLedgerService.creditStudent(student, payment, txType, description);

        log.info("Recorded payment {} for student {}", receiptNumber, student.getId());

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(Long studentId, String search, int page, int size) {
        return paymentRepository.searchPayments(studentId, search, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .studentId(payment.getStudent().getId())
                .studentName(payment.getStudent().getFullName())
                .admissionNumber(payment.getStudent().getAdmissionNumber())
                .appliedSemesterId(payment.getAppliedSemester() != null ? payment.getAppliedSemester().getId() : null)
                .appliedSemesterName(payment.getAppliedSemester() != null ? payment.getAppliedSemester().getName() : null)
                .receiptNumber(payment.getReceiptNumber())
                .amount(payment.getAmount())
                .gatewayReference(payment.getGatewayReference())
                .paymentMethod(payment.getPaymentMethod())
                .paidAt(payment.getPaidAt())
                .recordedByName(payment.getRecordedBy() != null ? payment.getRecordedBy().getFullName()  : "System")
                .status(payment.getStatus().name())
                .remarks(payment.getRemarks())
                .build();
    }
}
