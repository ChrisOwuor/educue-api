package com.owuor.educue.finance.service;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
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
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final UserRepository userRepository;
    private final FeeLedgerService feeLedgerService;
    private final FinanceDocumentNumberService documentNumbers;

    /**
     * Records a new payment, marks it as VERIFIED, and instantly credits the student's ledger.
     */
    public PaymentResponse recordPayment(RecordPaymentRequest request, Long recordedById) {
        return recordPaymentInternal(request, recordedById);
    }

    /** Records a provider-confirmed payment without impersonating a staff user. */
    public PaymentResponse recordProviderPayment(RecordPaymentRequest request) {
        return recordPaymentInternal(request, null);
    }

    private PaymentResponse recordPaymentInternal(RecordPaymentRequest request, Long recordedById) {

        if (request.getPaidAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future.");
        }

        if (paymentRepository.existsByGatewayReference(request.getGatewayReference())) {
            throw new IllegalArgumentException("A payment with this gateway reference already exists.");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        if (request.getPayerType() != com.owuor.educue.finance.enums.PayerType.STUDENT
                && (request.getPayerName() == null || request.getPayerName().isBlank())) {
            throw new IllegalArgumentException("Payer name is required for sponsor, loan and bursary payments.");
        }

        CourseAcademicPeriod appliedPeriod = null;
        if (request.getAppliedCourseAcademicPeriodUuid() != null) {
            appliedPeriod = courseAcademicPeriodRepository.findByUuid(request.getAppliedCourseAcademicPeriodUuid())
                    .orElseThrow(() -> new EntityNotFoundException("Course academic period not found"));
        }

        User recordedBy = recordedById == null ? null : userRepository.findById(recordedById)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Generate unique internal receipt number
        String receiptNumber = documentNumbers.paymentReceipt();

        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setAppliedCourseAcademicPeriod(appliedPeriod);
        payment.setAmount(request.getAmount());
        payment.setPayerType(request.getPayerType());
        payment.setPayerName(request.getPayerType() == com.owuor.educue.finance.enums.PayerType.STUDENT
                ? student.getFullName() : request.getPayerName().trim());
        payment.setGatewayReference(request.getGatewayReference());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaidAt(request.getPaidAt());
        payment.setReceiptNumber(receiptNumber);
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setRecordedBy(recordedBy);
        payment.setRemarks(request.getRemarks());

        payment = paymentRepository.save(payment);

        // Instantly reconcile the ledger
        TransactionType txType = switch (request.getPayerType()) {
            case SPONSOR -> TransactionType.SPONSOR_CREDIT;
            case GOVERNMENT_LOAN -> TransactionType.LOAN_CREDIT;
            case BURSARY_PROVIDER -> TransactionType.BURSARY_ALLOCATION;
            case STUDENT -> request.getPaymentMethod().equalsIgnoreCase("MPESA")
                    ? TransactionType.PAYMENT_MPESA : TransactionType.PAYMENT_BANK;
        };

        String detail = request.getRemarks() == null || request.getRemarks().isBlank()
                ? "" : " - " + request.getRemarks().trim();
        String description = switch (request.getPayerType()) {
            case STUDENT -> "Receipt Ref No." + payment.getReceiptNumber() + " - " + request.getGatewayReference();
            case SPONSOR -> "SPONSOR " + payment.getPayerName() + detail + " - Receipt Ref No." + payment.getReceiptNumber();
            case GOVERNMENT_LOAN -> "HELB LOAN" + detail + " - Receipt Ref No." + payment.getReceiptNumber();
            case BURSARY_PROVIDER -> "BURSARY " + payment.getPayerName() + detail + " - Receipt Ref No." + payment.getReceiptNumber();
        };

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
                .appliedCourseAcademicPeriodUuid(payment.getAppliedCourseAcademicPeriod() != null ? payment.getAppliedCourseAcademicPeriod().getUuid() : null)
                .appliedAcademicPeriodName(payment.getAppliedCourseAcademicPeriod() != null ? payment.getAppliedCourseAcademicPeriod().getAcademicPeriod().getName() : null)
                .receiptNumber(payment.getReceiptNumber())
                .amount(payment.getAmount())
                .payerType(payment.getPayerType().name())
                .payerName(payment.getPayerName())
                .gatewayReference(payment.getGatewayReference())
                .paymentMethod(payment.getPaymentMethod())
                .paidAt(payment.getPaidAt())
                .recordedByName(payment.getRecordedBy() != null ? payment.getRecordedBy().getFullName()  : "System")
                .status(payment.getStatus().name())
                .remarks(payment.getRemarks())
                .build();
    }
}
