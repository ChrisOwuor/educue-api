package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.FeeLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FeeLedgerRepository extends JpaRepository<FeeLedger, Long> {

    // Finds the most recent ledger row for a student to get their current running balance.
    Optional<FeeLedger> findTopByStudentIdOrderByIdDesc(Long studentId);

    // Finds all ledger rows for a student, ordered chronologically.
    List<FeeLedger> findAllByStudentIdOrderByIdAsc(Long studentId);

    // Checks if a student has already been billed for a specific fee structure.
    boolean existsByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);


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
            AND fl.semester.id = :semesterId
            """)
    BigDecimal getSemesterCharges(
            Long studentId,
            Long semesterId
    );

    @Query("""
SELECT COALESCE(SUM(fl.credit),0)
FROM FeeLedger fl
WHERE fl.student.id = :studentId
AND fl.semester.id = :semesterId
""")
    BigDecimal getSemesterPayments(
            Long studentId,
            Long semesterId
    );
}
