package com.owuor.educue.finance.service;

import com.owuor.educue.finance.dto.ChargeStudentRequest;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.entity.FeeLedger;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.entity.Payment;
import com.owuor.educue.finance.enums.TransactionType;
import com.owuor.educue.finance.repository.FeeLedgerRepository;
import com.owuor.educue.finance.repository.FeeStructureRepository;
import com.owuor.educue.students.entity.Student;
import com.owuor.educue.students.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeeLedgerService {

    private final FeeLedgerRepository feeLedgerRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentRepository studentRepository;

    /**
     * Charges a student based on a fee structure.
     * Creates a TUITION_BILL entry in the fee_ledger and updates the running balance.
     */
    public FeeLedgerResponse chargeStudent(ChargeStudentRequest request) {

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        FeeStructure feeStructure = feeStructureRepository.findWithItemsById(request.getFeeStructureId())
                .orElseThrow(() -> new EntityNotFoundException("Fee structure not found"));

        // Prevent duplicate billing for the same fee structure
        if (feeLedgerRepository.existsByStudentIdAndFeeStructureId(student.getId(), feeStructure.getId())) {
            throw new IllegalArgumentException("Student has already been billed for this fee structure.");
        }

        // Get the student's current running balance (0 if no ledger rows exist yet)
        BigDecimal currentBalance = feeLedgerRepository
                .findTopByStudentIdOrderByIdDesc(student.getId())
                .map(FeeLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal debitAmount = feeStructure.getTotalAmount();

        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setSemester(feeStructure.getSemester());
        entry.setTransactionType(TransactionType.TUITION_BILL);
        entry.setFeeStructure(feeStructure);
        entry.setDebit(debitAmount);
        entry.setRunningBalance(currentBalance.add(debitAmount));
        entry.setDescription(
                feeStructure.getCourse().getName() + " – " +
                feeStructure.getSemester().getName() + " (" +
                feeStructure.getIntake().getName() + ")"
        );

        entry = feeLedgerRepository.save(entry);

        log.info("Charged student {} with {} for fee structure {}",
                student.getId(), debitAmount, feeStructure.getId());

        return toResponse(entry);
    }

    /**
     * Credits a student's ledger (e.g., from a recorded payment).
     * Reduces the running balance.
     */
    public FeeLedgerResponse creditStudent(Student student, Payment payment, TransactionType type, String description) {
        
        BigDecimal currentBalance = getStudentBalance(student.getId());
        BigDecimal creditAmount = payment.getAmount();

        FeeLedger entry = new FeeLedger();
        entry.setStudent(student);
        entry.setSemester(payment.getAppliedSemester()); // Can be null if not tied to specific semester
        entry.setTransactionType(type);
        entry.setPayment(payment);
        entry.setCredit(creditAmount);
        // Reduce debt: runningBalance = current - credit
        entry.setRunningBalance(currentBalance.subtract(creditAmount)); 
        entry.setDescription(description);

        entry = feeLedgerRepository.save(entry);

        log.info("Credited student {} with {} for payment {}", student.getId(), creditAmount, payment.getId());

        return toResponse(entry);
    }

    /**
     * Returns all ledger entries for a given student, ordered chronologically.
     */
    @Transactional(readOnly = true)
    public List<FeeLedgerResponse> getStudentLedger(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new EntityNotFoundException("Student not found");
        }

        return feeLedgerRepository.findAllByStudentIdOrderByIdAsc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
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
//    public void validateUnitRegistrationEligibilityV1(
//            Long studentId,
//            Long semesterId
//    ) {
//
//        BigDecimal charges =
//                feeLedgerRepository.getSemesterCharges(studentId, semesterId);
//
//        BigDecimal payments =
//                feeLedgerRepository.getSemesterPayments(studentId, semesterId);
//
//        BigDecimal required =
//                charges.multiply(new BigDecimal("0.50"));
//
//        if (payments.compareTo(required) < 0) {
//            throw new IllegalStateException(
//                    "You must pay at least 50% of this semester's fees before registering units."
//            );
//        }
//    }


    @Transactional(readOnly = true)
    public void validateUnitRegistrationEligibility(
            Long studentId,
            Long feeStructureId
    ) {

        FeeStructure feeStructure = feeStructureRepository.findById(feeStructureId)
                .orElseThrow(() -> new IllegalArgumentException("Fee structure not found."));

        // Total fee for the current semester
        BigDecimal semesterFee = feeStructure.getTotalAmount();

        // Institution policy (50%)
        BigDecimal requiredPayment =
                semesterFee.multiply(new BigDecimal("0.50"));

        /*
         * Maximum debt a student is allowed to have
         * after satisfying the 50% rule.
         *
         * Example:
         * Semester Fee = 60,000
         * Required = 30,000
         * Max Outstanding = 30,000
         */
        BigDecimal maximumOutstandingAllowed =
                semesterFee.subtract(requiredPayment);

        /*
         * Latest running balance.
         *
         * Positive  -> Student owes money.
         * Zero      -> Fully cleared.
         * Negative  -> Student has credit.
         */
        BigDecimal currentOutstanding = feeLedgerRepository
                .findTopByStudentIdOrderByIdDesc(studentId)
                .map(FeeLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO);

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
        return feeLedgerRepository
                .findTopByStudentIdOrderByIdDesc(studentId)
                .map(FeeLedger::getRunningBalance)
                .orElse(BigDecimal.ZERO);
    }

    public FeeLedgerResponse toResponse(FeeLedger entry) {
        return FeeLedgerResponse.builder()
                .id(entry.getId())
                .studentId(entry.getStudent().getId())
                .studentName(
                        entry.getStudent().getFullName()
                )
                .semesterId(entry.getSemester() != null ? entry.getSemester().getId() : null)
                .semesterName(entry.getSemester() != null ? entry.getSemester().getName() : null)
                .transactionType(entry.getTransactionType().name())
                .debit(entry.getDebit())
                .credit(entry.getCredit())
                .runningBalance(entry.getRunningBalance())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
