package com.owuor.educue.finance.service;

import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.entity.FeeLedger;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.entity.Payment;
import com.owuor.educue.finance.enums.TransactionType;
import com.owuor.educue.finance.repository.FeeLedgerRepository;
import com.owuor.educue.finance.repository.FeeStructureRepository;
import com.owuor.educue.finance.enums.LedgerStatus;
import com.owuor.educue.finance.enums.PaymentStatus;
import com.owuor.educue.finance.enums.DebitReason;
import com.owuor.educue.finance.dto.DebitStudentRequest;
import com.owuor.educue.finance.dto.ReverseLedgerEntryRequest;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.users.repository.UserRepository;
import com.owuor.educue.students.entity.Student;
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
    private final FeeStructureRepository feeStructureRepository;
    private final StudentRepository studentRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final UserRepository userRepository;
    private final FinanceDocumentNumberService documentNumbers;

    /**
     * Charges a student based on a fee structure.
     * Creates a TUITION_BILL entry in the fee_ledger and updates the running balance.
     */
    public FeeLedgerResponse billEnrollmentPeriod(Student student, FeeStructure feeStructure) {

        // Prevent duplicate billing for the same fee structure
        if (feeLedgerRepository.existsByStudentIdAndFeeStructureId(student.getId(), feeStructure.getId())) {
            throw new IllegalArgumentException("Student has already been billed for this fee structure.");
        }

        BigDecimal debitAmount = feeStructure.getTotalAmount();

        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setCourseAcademicPeriod(feeStructure.getCourseAcademicPeriod());
        entry.setTransactionType(TransactionType.TUITION_BILL);
        entry.setFeeStructure(feeStructure);
        entry.setDebit(debitAmount);
        entry.setPostingDate(LocalDate.now());
        entry.setDocumentNumber(documentNumbers.invoice());
        entry.setDescription("Fees Invoice For " + feeStructure.getCourseAcademicPeriod().getAcademicPeriod().getCode());
        entry = feeLedgerRepository.save(entry);

        log.info("Charged student {} with {} for fee structure {}",
                student.getId(), debitAmount, feeStructure.getId());

        return toResponse(entry, feeLedgerRepository.getOutstandingBalance(student.getId()));
    }

    /**
     * Credits a student's ledger (e.g., from a recorded payment).
     * Reduces the running balance.
     */
    public FeeLedgerResponse creditStudent(Student student, Payment payment, TransactionType type, String description) {
        
        BigDecimal creditAmount = payment.getAmount();

        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setCourseAcademicPeriod(payment.getAppliedCourseAcademicPeriod());
        entry.setTransactionType(type);
        entry.setPayment(payment);
        entry.setCredit(creditAmount);
        // The authoritative balance is derived from the immutable ledger sum.
        entry.setPostingDate(payment.getPaidAt().toLocalDate());
        entry.setDocumentNumber(payment.getPayerType() == com.owuor.educue.finance.enums.PayerType.STUDENT
                ? documentNumbers.paymentPosting() : documentNumbers.creditNote());
        entry.setExternalReference(payment.getGatewayReference());
        entry.setDescription(description);

        entry = feeLedgerRepository.save(entry);

        log.info("Credited student {} with {} for payment {}", student.getId(), creditAmount, payment.getId());

        return toResponse(entry, feeLedgerRepository.getOutstandingBalance(student.getId()));
    }

    /** Posts a controlled manual debit such as a resit, retake or penalty charge. */
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

    /** Reverses an entry by posting its exact opposite; the original is never edited or deleted. */
    public FeeLedgerResponse reverse(Long ledgerId, ReverseLedgerEntryRequest request, Long userId) {
        FeeLedger original = feeLedgerRepository.findById(ledgerId)
                .orElseThrow(() -> new EntityNotFoundException("Ledger entry not found"));
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
        reversal.setDocumentNumber(documentNumbers.creditNote());
        reversal.setDescription("REVERSE " + original.getDocumentNumber() + " - " + original.getDescription()
                + " (" + request.reason().trim() + ")");
        reversal.setReversalOf(original);
        reversal.setPayment(original.getPayment());
        reversal.setCreatedBy(userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found")));
        original.setStatus(LedgerStatus.REVERSED);

        // A payment and the ledger credit it produced are one accounting event.
        // Reversing that credit must also invalidate the source receipt so payment
        // reports cannot continue presenting it as verified money received.
        if (original.getPayment() != null) {
            if (original.getPayment().getStatus() == PaymentStatus.REVERSED) {
                throw new IllegalArgumentException("The associated payment has already been reversed");
            }
            original.getPayment().setStatus(PaymentStatus.REVERSED);
        }
        reversal = feeLedgerRepository.save(reversal);
        return toResponse(reversal, feeLedgerRepository.getOutstandingBalance(original.getStudent().getId()));
    }

    /**
     * Returns all ledger entries for a given student, ordered chronologically.
     */
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
        var balances = new java.util.HashMap<Long, BigDecimal>();
        var entries = feeLedgerRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.asc("student.id"),
                org.springframework.data.domain.Sort.Order.asc("postingDate"),
                org.springframework.data.domain.Sort.Order.asc("createdAt"),
                org.springframework.data.domain.Sort.Order.asc("id")));
        return entries.stream().map(entry -> {
            BigDecimal balance = balances.getOrDefault(entry.getStudent().getId(), BigDecimal.ZERO)
                    .add(entry.getDebit()).subtract(entry.getCredit());
            balances.put(entry.getStudent().getId(), balance);
            return toResponse(entry, balance);
        }).toList();
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

    @Transactional(readOnly = true)
    public void validateUnitRegistrationEligibility(
            Long studentId,
            Long feeStructureId
    ) {

        FeeStructure feeStructure = feeStructureRepository.findById(feeStructureId)
                .orElseThrow(() -> new IllegalArgumentException("Fee structure not found."));

        // Total fee for the enrollment's current academic period.
        BigDecimal periodFee = feeStructure.getTotalAmount();

        // Institution policy (50%)
        BigDecimal requiredPayment =
                periodFee.multiply(new BigDecimal("0.50"));

        /*
         * Maximum debt a student is allowed to have
         * after satisfying the 50% rule.
         *
         * Example:
         * Period Fee = 60,000
         * Required = 30,000
         * Max Outstanding = 30,000
         */
        BigDecimal maximumOutstandingAllowed =
                periodFee.subtract(requiredPayment);

        /*
         * Latest running balance.
         *
         * Positive  -> Student owes money.
         * Zero      -> Fully cleared.
         * Negative  -> Student has credit.
         */
        BigDecimal currentOutstanding = feeLedgerRepository.getOutstandingBalance(studentId);

        if (currentOutstanding.compareTo(maximumOutstandingAllowed) > 0) {
            BigDecimal minimumRequired =
                    currentOutstanding.subtract(maximumOutstandingAllowed);

            throw new IllegalStateException(
                    "You must pay at least KES " +
                    minimumRequired +
                    " before registering units."
            );
        }
    }

    /**
     * Returns the current running balance for a student.
     */
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
                .studentName(
                        entry.getStudent().getFullName()
                )
                .courseAcademicPeriodUuid(entry.getCourseAcademicPeriod() != null ? entry.getCourseAcademicPeriod().getUuid() : null)
                .academicPeriodName(entry.getCourseAcademicPeriod() != null ? entry.getCourseAcademicPeriod().getAcademicPeriod().getName() : null)
                .transactionType(entry.getTransactionType().name())
                .postingDate(entry.getPostingDate())
                .documentNumber(entry.getDocumentNumber())
                .externalReference(entry.getExternalReference())
                .status(entry.getStatus().name())
                .reversalOfId(entry.getReversalOf() == null ? null : entry.getReversalOf().getId())
                .debit(entry.getDebit())
                .credit(entry.getCredit())
                .runningBalance(runningBalance)
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
