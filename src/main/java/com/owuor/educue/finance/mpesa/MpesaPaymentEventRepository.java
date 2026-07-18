package com.owuor.educue.finance.mpesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public interface MpesaPaymentEventRepository extends JpaRepository<MpesaPaymentEvent, Long> {
    boolean existsByTransactionId(String transactionId);

    @Modifying
    @Query(value = """
        INSERT INTO mpesa_payment_events
          (transaction_id, transaction_type, transaction_time, amount, business_short_code,
           account_reference, invoice_number, third_party_transaction_id, phone_hash, phone_last_four)
        VALUES (:transactionId, :transactionType, :transactionTime, :amount, :shortCode,
                :accountReference, :invoiceNumber, :thirdPartyId, :phoneHash, :phoneLastFour)
        ON CONFLICT (transaction_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(String transactionId, String transactionType, LocalDateTime transactionTime,
            BigDecimal amount, String shortCode, String accountReference, String invoiceNumber,
            String thirdPartyId, String phoneHash, String phoneLastFour);

    @Query(value = """
        WITH candidates AS (
          SELECT id FROM mpesa_payment_events
          WHERE status IN ('RECEIVED','RETRY') AND next_attempt_at <= CURRENT_TIMESTAMP
          ORDER BY received_at, id FOR UPDATE SKIP LOCKED LIMIT :limit
        )
        UPDATE mpesa_payment_events e SET status='PROCESSING',
          processing_started_at=CURRENT_TIMESTAMP, attempt_count=attempt_count+1
        FROM candidates c WHERE e.id=c.id RETURNING e.id
        """, nativeQuery = true)
    List<Long> claimBatch(int limit);

    @Modifying
    @Query(value = """
        UPDATE mpesa_payment_events SET status='RETRY', processing_started_at=NULL,
        next_attempt_at=CURRENT_TIMESTAMP WHERE status='PROCESSING' AND processing_started_at < :cutoff
        """, nativeQuery = true)
    int recoverStale(LocalDateTime cutoff);
}
