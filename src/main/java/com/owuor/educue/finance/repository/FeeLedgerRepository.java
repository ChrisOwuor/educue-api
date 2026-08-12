package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.FeeLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.owuor.educue.finance.enums.TransactionType;

public interface FeeLedgerRepository extends JpaRepository<FeeLedger, Long> {

    @Query(value = "SELECT nextval('finance_document_number_seq')", nativeQuery = true)
    Long nextDocumentSequence();

    @Query(value = "SELECT nextval('finance_receipt_number_seq')", nativeQuery = true)
    Long nextReceiptSequence();

    boolean existsByReversalOfId(Long ledgerId);
    boolean existsByExternalReference(String externalReference);
    boolean existsByStudentIdAndTransactionType(Long studentId, TransactionType transactionType);

    Optional<FeeLedger> findTopByStudentIdOrderByIdDesc(Long studentId);
    Optional<FeeLedger> findByPaymentId(Long paymentId);

    List<FeeLedger> findAllByStudentIdOrderByIdAsc(Long studentId);

    List<FeeLedger> findAllByStudentIdOrderByPostingDateAscCreatedAtAscIdAsc(Long studentId);

    @Query("SELECT COUNT(fl) > 0 FROM FeeLedger fl WHERE fl.student.id = :studentId AND fl.courseAcademicPeriod.id = :courseAcademicPeriodId AND fl.transactionType = 'TUITION_BILL'")
    boolean existsTuitionBillForPeriod(@Param("studentId") Long studentId, @Param("courseAcademicPeriodId") Long courseAcademicPeriodId);
    
    @Query("""
            SELECT COALESCE(SUM(fl.debit - fl.credit), 0)
            FROM FeeLedger fl
            WHERE fl.student.id = :studentId
            """)
    BigDecimal getOutstandingBalance(@Param("studentId") Long studentId);


    @Query("""
            SELECT COALESCE(SUM(fl.debit), 0)
            FROM FeeLedger fl
            WHERE fl.student.id = :studentId
            """)
    BigDecimal getTotalCharges(@Param("studentId") Long studentId);

    @Query("""
            SELECT COALESCE(SUM(fl.credit), 0)
            FROM FeeLedger fl
            WHERE fl.student.id = :studentId
            """)
    BigDecimal getTotalPayments(@Param("studentId") Long studentId);


    @Query("""
            SELECT COALESCE(SUM(fl.debit),0)
            FROM FeeLedger fl
            WHERE fl.student.id = :studentId
            AND fl.courseAcademicPeriod.id = :courseAcademicPeriodId
            """)
    BigDecimal getAcademicPeriodCharges(
            Long studentId,
            Long courseAcademicPeriodId
    );

    @Query("""
SELECT COALESCE(SUM(fl.credit),0)
FROM FeeLedger fl
WHERE fl.student.id = :studentId
AND fl.courseAcademicPeriod.id = :courseAcademicPeriodId
""")
    BigDecimal getAcademicPeriodPayments(
            Long studentId,
            Long courseAcademicPeriodId
    );
}
