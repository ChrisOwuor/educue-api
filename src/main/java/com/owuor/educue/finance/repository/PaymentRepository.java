package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByGatewayReference(String gatewayReference);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    @Query("""
            SELECT p
            FROM Payment p
            ORDER BY p.paidAt DESC
            """)
    Page<Payment> searchPayments(
            @Param("studentId") Long studentId,
            @Param("search") String search,
            Pageable pageable
    );
}
