package com.owuor.educue.finance.service;

import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.entity.FeeLedger;
import com.owuor.educue.finance.entity.Payment;
import com.owuor.educue.finance.enums.TransactionType;
import com.owuor.educue.finance.repository.FeeLedgerRepository;
import com.owuor.educue.finance.enums.LedgerStatus;
import com.owuor.educue.finance.enums.PaymentStatus;
import com.owuor.educue.finance.enums.DebitReason;
import com.owuor.educue.finance.dto.DebitStudentRequest;
import com.owuor.educue.finance.dto.ReverseLedgerEntryRequest;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.users.repository.UserRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.students.entity.Student;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeeLedgerService {

    private final FeeLedgerRepository feeLedgerRepository;
    private final StudentRepository studentRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final UserRepository userRepository;
    private final FinanceDocumentNumberService documentNumbers;

    public void billEnrollmentPeriod(
            Enrollment enrollment,
            BigDecimal periodFee
    ) {
        Student student = enrollment.getStudent();

        if (periodFee == null || periodFee.signum() <= 0) {
            throw new IllegalArgumentException(
                    "The enrollment period fee must be greater than zero"
            );
        }

        Long courseAcademicPeriodId =
                enrollment
                        .getCurrentCourseAcademicPeriod()
                        .getId();

        if (feeLedgerRepository.existsTuitionBillForPeriod(
                student.getId(),
                courseAcademicPeriodId
        )) {
            throw new IllegalArgumentException(
                    "Student has already been billed for this course period."
            );
        }

        FeeLedger entry = new FeeLedger();

        entry.setStudent(student);
        entry.setCourseAcademicPeriod(
                enrollment.getCurrentCourseAcademicPeriod()
        );

        entry.setTransactionType(
                TransactionType.TUITION_BILL
        );

        entry.setDebit(periodFee);
        entry.setPostingDate(LocalDate.now());
        entry.setDocumentNumber(
                documentNumbers.invoice()
        );

        entry.setDescription(
                "Fees Invoice For "
                + enrollment.getCurrentAcademicYear().getCode()
                + " "
                + enrollment
                        .getCurrentCourseAcademicPeriod()
                        .getAcademicPeriod()
                        .getCode()
        );

        entry = feeLedgerRepository.save(entry);

        log.info(
                "Charged student {} with {} for course period {}",
                student.getId(),
                periodFee,
                courseAcademicPeriodId
        );

        toResponse(
                entry,
                feeLedgerRepository.getOutstandingBalance(
                        student.getId()
                )
        );
    }

    public FeeLedger billGraduation(Enrollment enrollment, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Graduation fee total must be greater than zero");
        }
        FeeLedger entry = new FeeLedger();
        entry.setStudent(enrollment.getStudent());
        entry.setCourseAcademicPeriod(enrollment.getCurrentCourseAcademicPeriod());
        entry.setTransactionType(TransactionType.GRADUATION_FEE);
        entry.setDebit(amount);
        entry.setPostingDate(LocalDate.now());
        entry.setDocumentNumber(documentNumbers.invoice());
        entry.setDescription("Graduation fees for " + enrollment.getCourse().getCode());
        return feeLedgerRepository.save(entry);
    }

    public FeeLedger billUnitAttempt(Enrollment enrollment, com.owuor.educue.students.entity.StudentUnitRegistration registration, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Attempt fee must be greater than zero");
        FeeLedger entry = new FeeLedger();
        entry.setStudent(enrollment.getStudent());
        entry.setCourseAcademicPeriod(registration.getCourseUnitPlacement().getCourseAcademicPeriod());
        entry.setTransactionType(registration.getAttemptType() == com.owuor.educue.academics.enums.AttemptType.RETAKE
                ? TransactionType.RETAKE_FEE : TransactionType.RESIT_FEE);
        entry.setDebit(amount);
        entry.setPostingDate(LocalDate.now());
        entry.setDocumentNumber(documentNumbers.invoice());
        entry.setExternalReference("UNIT-ATTEMPT-" + registration.getId());
        entry.setDescription((registration.getAttemptType() == com.owuor.educue.academics.enums.AttemptType.RETAKE ? "Retake" : "Supplementary")
                + " fee for " + registration.getCourseUnitPlacement().getUnit().getCode());
        return feeLedgerRepository.save(entry);
    }

    public FeeLedger postDebitClaim(Student student, BigDecimal amount, String category, String reason, String claimReference, User officer) {
        if (feeLedgerRepository.existsByExternalReference(claimReference)) return null;
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Claim amount must be greater than zero");
        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setTransactionType("ACCOMMODATION".equalsIgnoreCase(category) ? TransactionType.ACCOMMODATION_BILL : TransactionType.PENALTY_FEE);
        entry.setDebit(amount); entry.setPostingDate(LocalDate.now()); entry.setDocumentNumber(documentNumbers.debitNote());
        entry.setExternalReference(claimReference); entry.setDescription(category.replace('_',' ') + " claim: " + reason); entry.setCreatedBy(officer);
        return feeLedgerRepository.save(entry);
    }

    public FeeLedgerResponse postMigrationOpeningBalance(Student student, BigDecimal signedBalance,
                                                          LocalDate postingDate, String externalReference) {
        if (signedBalance == null || signedBalance.signum() == 0) return null;
        if (!feeLedgerRepository.findAllByStudentIdOrderByIdAsc(student.getId()).isEmpty())
            throw new IllegalArgumentException("An opening balance can only be posted to an empty student ledger");
        validatePostingDate(postingDate);
        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setPostingDate(postingDate);
        entry.setDocumentNumber(signedBalance.signum() > 0 ? documentNumbers.debitNote() : documentNumbers.creditNote());
        entry.setExternalReference(externalReference == null || externalReference.isBlank() ? null : externalReference.trim());
        entry.setDescription("Legacy system opening balance");
        if (signedBalance.signum() > 0) {
            entry.setTransactionType(TransactionType.MIGRATION_OPENING_DEBIT);
            entry.setDebit(signedBalance);
        } else {
            entry.setTransactionType(TransactionType.MIGRATION_OPENING_CREDIT);
            entry.setCredit(signedBalance.abs());
        }
        entry = feeLedgerRepository.save(entry);
        return toResponse(entry, signedBalance);
    }

    public FeeLedgerResponse creditStudent(Student student, Payment payment, TransactionType type, String description) {
        
        BigDecimal creditAmount = payment.getAmount();

        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setCourseAcademicPeriod(payment.getAppliedCourseAcademicPeriod());
        entry.setTransactionType(type);
        entry.setPayment(payment);
        entry.setCredit(creditAmount);
        entry.setPostingDate(payment.getPaidAt().toLocalDate());
        entry.setDocumentNumber(payment.getPayerType() == com.owuor.educue.finance.enums.PayerType.STUDENT
                ? documentNumbers.paymentPosting() : documentNumbers.creditNote());
        entry.setExternalReference(payment.getGatewayReference());
        entry.setDescription(description);

        entry = feeLedgerRepository.save(entry);

        log.info("Credited student {} with {} for payment {}", student.getId(), creditAmount, payment.getId());

        return toResponse(entry, feeLedgerRepository.getOutstandingBalance(student.getId()));
    }

    public FeeLedgerResponse debitStudent(DebitStudentRequest request, Long userId) {
        validatePostingDate(request.postingDate());
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));
        var period = request.courseAcademicPeriodUuid() == null ? null
                : courseAcademicPeriodRepository.findByUuid(request.courseAcademicPeriodUuid())
                .orElseThrow(() -> new EntityNotFoundException("Course academic period not found"));
        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setCourseAcademicPeriod(period);
        entry.setTransactionType(switch (request.reason()) {
            case RESIT_FEE -> TransactionType.RESIT_FEE;
            case RETAKE_FEE -> TransactionType.RETAKE_FEE;
            case PENALTY_FEE -> TransactionType.PENALTY_FEE;
            case OTHER -> TransactionType.MANUAL_DEBIT;
        });
        entry.setDebit(request.amount());
        entry.setPostingDate(request.postingDate());
        entry.setDocumentNumber(documentNumbers.debitNote());
        entry.setExternalReference(request.externalReference());
        entry.setDescription(descriptionFor(request.reason(), request.details()));
        entry.setCreatedBy(userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found")));
        entry = feeLedgerRepository.save(entry);
        return toResponse(entry, feeLedgerRepository.getOutstandingBalance(student.getId()));
    }

    public FeeLedgerResponse reverse(Long ledgerId, ReverseLedgerEntryRequest request, Long userId) {
        FeeLedger original = feeLedgerRepository.findById(ledgerId)
                .orElseThrow(() -> new EntityNotFoundException("Ledger entry not found"));
        if (original.getPayment() != null) {
            throw new IllegalArgumentException("Payment-backed credits must be reversed from Transactions");
        }
        if (original.getCredit().signum() > 0 && original.getTransactionType() != TransactionType.MIGRATION_OPENING_CREDIT) {
            throw new IllegalArgumentException("Only migrated opening credits can be reversed directly from the ledger");
        }
        return reverseEntry(original, request, userId, false);
    }

    public FeeLedgerResponse reversePayment(Payment payment, ReverseLedgerEntryRequest request, Long userId) {
        FeeLedger original = feeLedgerRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment ledger entry not found"));
        return reverseEntry(original, request, userId, true);
    }

    private FeeLedgerResponse reverseEntry(FeeLedger original, ReverseLedgerEntryRequest request, Long userId, boolean paymentReversal) {
        Long ledgerId = original.getId();
        if (original.getStatus() == LedgerStatus.REVERSED || feeLedgerRepository.existsByReversalOfId(ledgerId)) {
            throw new IllegalArgumentException("This ledger entry has already been reversed");
        }
        validatePostingDate(request.postingDate());
        if (request.postingDate().isBefore(original.getPostingDate())) {
            throw new IllegalArgumentException("Reversal date cannot be before the original posting date");
        }
        FeeLedger reversal = new FeeLedger();
        reversal.setStudent(original.getStudent());
        reversal.setCourseAcademicPeriod(original.getCourseAcademicPeriod());
        reversal.setTransactionType(TransactionType.REVERSAL);
        reversal.setDebit(original.getCredit());
        reversal.setCredit(original.getDebit());
        reversal.setPostingDate(request.postingDate());
        reversal.setDocumentNumber(original.getDebit().signum() > 0 ? documentNumbers.creditNote() : documentNumbers.debitNote());
        reversal.setDescription("REVERSE " + original.getDocumentNumber() + " - " + original.getDescription()
                + " (" + request.reason().trim() + ")");
        reversal.setReversalOf(original);
        reversal.setPayment(original.getPayment());
        reversal.setCreatedBy(userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found")));
        original.setStatus(LedgerStatus.REVERSED);

        if (paymentReversal && original.getPayment() != null) {
            if (original.getPayment().getStatus() == PaymentStatus.REVERSED) {
                throw new IllegalArgumentException("The associated payment has already been reversed");
            }
            original.getPayment().setStatus(PaymentStatus.REVERSED);
        }
        reversal = feeLedgerRepository.save(reversal);
        return toResponse(reversal, feeLedgerRepository.getOutstandingBalance(original.getStudent().getId()));
    }

    @Transactional(readOnly = true)
    public List<FeeLedgerResponse> getStudentLedger(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new EntityNotFoundException("Student not found");
        }
        BigDecimal balance = BigDecimal.ZERO;
        List<FeeLedgerResponse> statement = new java.util.ArrayList<>();
        for (FeeLedger entry : feeLedgerRepository.findAllByStudentIdOrderByPostingDateAscCreatedAtAscIdAsc(studentId)) {
            balance = balance.add(entry.getDebit()).subtract(entry.getCredit());
            statement.add(toResponse(entry, balance));
        }
        return statement;
    }

    @Transactional(readOnly = true)
    public List<FeeLedgerResponse> getAllLedgerEntries() {
        return getAllLedgerEntries(null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<FeeLedgerResponse> getAllLedgerEntries(
            String search,
            java.util.UUID academicPeriodUuid,
            TransactionType transactionType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        var balances = new java.util.HashMap<Long, BigDecimal>();
        var entries = feeLedgerRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.asc("student.id"),
                org.springframework.data.domain.Sort.Order.asc("postingDate"),
                org.springframework.data.domain.Sort.Order.asc("createdAt"),
                org.springframework.data.domain.Sort.Order.asc("id")));
        var completeLedger = entries.stream().map(entry -> {
            BigDecimal balance = balances.getOrDefault(entry.getStudent().getId(), BigDecimal.ZERO)
                    .add(entry.getDebit()).subtract(entry.getCredit());
            balances.put(entry.getStudent().getId(), balance);
            return toResponse(entry, balance);
        }).toList();
        String term = search == null ? "" : search.trim().toLowerCase();
        return completeLedger.stream().filter(entry ->
                (term.isEmpty()
                        || contains(entry.getAdmissionNumber(), term)
                        || contains(entry.getDocumentNumber(), term)
                        || contains(entry.getExternalReference(), term)
                        || contains(entry.getDescription(), term))
                && (academicPeriodUuid == null || academicPeriodUuid.equals(entry.getAcademicPeriodUuid()))
                && (transactionType == null || transactionType.name().equals(entry.getTransactionType()))
                && (fromDate == null || !entry.getPostingDate().isBefore(fromDate))
                && (toDate == null || !entry.getPostingDate().isAfter(toDate))
        ).toList();
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    @Transactional(readOnly = true)
    public void validateUnitRegistrationEligibility(Long studentId) {
        BigDecimal totalCharges = feeLedgerRepository.getTotalCharges(studentId);
        BigDecimal totalPayments = feeLedgerRepository.getTotalPayments(studentId);
        BigDecimal requiredAmount = totalCharges.multiply(new BigDecimal("0.50"));

        if (totalPayments.compareTo(requiredAmount) < 0) {
            throw new IllegalStateException(
                    "You have not met the minimum fee payment required for unit registration."
            );
        }
    }

//    @Transactional(readOnly = true)
//    public void validateUnitRegistrationEligibility(Long studentId, Long feeStructureId) {
//        FeeStructure feeStructure = feeStructureRepository.findById(feeStructureId)
//                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found."));
//        BigDecimal periodFee = feeStructure.getTotalAmount();
//        BigDecimal requiredPayment = periodFee.multiply(new BigDecimal("0.50"));
//        BigDecimal maximumOutstandingAllowed = periodFee.subtract(requiredPayment);
//        BigDecimal currentOutstanding = feeLedgerRepository.getOutstandingBalance(studentId);
//
//        if (currentOutstanding.compareTo(maximumOutstandingAllowed) > 0) {
//            BigDecimal minimumRequired = currentOutstanding.subtract(maximumOutstandingAllowed);
//            throw new IllegalStateException(
//                    "You must pay at least KES " + minimumRequired + " before registering units."
//            );
//        }
//    }

    @Transactional(readOnly = true)
    public void validateUnitRegistrationEligibility(
            Long studentId,
            BigDecimal currentPeriodFee
    ) {
        if (currentPeriodFee == null
            || currentPeriodFee.signum() <= 0) {

            throw new IllegalStateException(
                    "No valid fee amount is configured for the student's current period."
            );
        }

        BigDecimal requiredPayment =
                currentPeriodFee.multiply(
                        new BigDecimal("0.50")
                );

        BigDecimal maximumOutstandingAllowed =
                currentPeriodFee.subtract(
                        requiredPayment
                );

        BigDecimal currentOutstanding =
                feeLedgerRepository.getOutstandingBalance(
                        studentId
                );

        if (currentOutstanding == null) {
            currentOutstanding = BigDecimal.ZERO;
        }

        if (currentOutstanding.compareTo(
                maximumOutstandingAllowed
        ) > 0) {
            BigDecimal minimumRequired =
                    currentOutstanding.subtract(
                            maximumOutstandingAllowed
                    );

            throw new IllegalStateException(
                    "You must pay at least KES "
                    + minimumRequired
                            .setScale(
                                    2,
                                    java.math.RoundingMode.HALF_UP
                            )
                            .toPlainString()
                    + " before registering units."
            );
        }
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getStudentBalance(Long studentId) {
        return feeLedgerRepository.getOutstandingBalance(studentId);
    }

    private String descriptionFor(DebitReason reason, String details) {
        String clean = details.trim().replaceAll("\\s+", " ");
        return switch (reason) {
            case RESIT_FEE -> "REG RESIT " + clean;
            case RETAKE_FEE -> "RETAKE " + clean;
            case PENALTY_FEE -> "PENALTY " + clean;
            case OTHER -> clean;
        };
    }

    private void validatePostingDate(LocalDate postingDate) {
        if (postingDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Posting date cannot be in the future");
        }
    }

    public FeeLedgerResponse toResponse(FeeLedger entry, BigDecimal runningBalance) {
        return FeeLedgerResponse.builder()
                .id(entry.getId())
                .studentId(entry.getStudent().getId())
                .studentName(entry.getStudent().getFullName())
                .admissionNumber(entry.getStudent().getAdmissionNumber())
                .courseAcademicPeriodUuid(entry.getCourseAcademicPeriod() != null ? entry.getCourseAcademicPeriod().getUuid() : null)
                .academicPeriodName(entry.getCourseAcademicPeriod() != null ? entry.getCourseAcademicPeriod().getAcademicPeriod().getName() : null)
                .academicPeriodCode(entry.getCourseAcademicPeriod() != null ? entry.getCourseAcademicPeriod().getAcademicPeriod().getCode() : null)
                .academicPeriodUuid(entry.getCourseAcademicPeriod() != null ? entry.getCourseAcademicPeriod().getAcademicPeriod().getUuid() : null)
                .transactionType(entry.getTransactionType().name())
                .postingDate(entry.getPostingDate())
                .documentNumber(entry.getDocumentNumber())
                .externalReference(entry.getExternalReference())
                .status(entry.getStatus().name())
                .reversalOfId(entry.getReversalOf() == null ? null : entry.getReversalOf().getId())
                .paymentId(entry.getPayment() == null ? null : entry.getPayment().getId())
                .debit(entry.getDebit())
                .credit(entry.getCredit())
                .runningBalance(runningBalance)
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
