package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    boolean existsByGatewayReference(String gatewayReference);

    Optional<Payment> findByReceiptNumber(String receiptNumber);
    Optional<Payment> findByGatewayReference(String gatewayReference);

    @Query("""
            SELECT p
            FROM Payment p
            WHERE (:studentId IS NULL OR p.student.id = :studentId)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(p.receiptNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.gatewayReference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.student.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:method IS NULL OR p.paymentMethod = :method)
              AND (:payerType IS NULL OR p.payerType = :payerType)
              AND (:status IS NULL OR p.status = :status)
              AND p.paidAt >= :fromDate
              AND p.paidAt < :toDate
            """)
    Page<Payment> searchPayments(
            @Param("studentId") Long studentId,
            @Param("search") String search,
            @Param("method") String method,
            @Param("payerType") com.owuor.educue.finance.enums.PayerType payerType,
            @Param("status") com.owuor.educue.finance.enums.PaymentStatus status,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable
    );
}
