package com.owuor.educue.finance.mpesa;

import com.owuor.educue.finance.entity.Payment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpesa_payment_events")
@Getter @Setter @NoArgsConstructor
public class MpesaPaymentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="transaction_id", nullable=false, updatable=false, length=40) private String transactionId;
    @Column(name="transaction_type", length=40) private String transactionType;
    @Column(name="transaction_time", nullable=false) private LocalDateTime transactionTime;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Column(name="business_short_code", nullable=false, length=20) private String businessShortCode;
    @Column(name="account_reference", nullable=false, length=80) private String accountReference;
    @Column(name="invoice_number", length=80) private String invoiceNumber;
    @Column(name="third_party_transaction_id", length=80) private String thirdPartyTransactionId;
    @Column(name="phone_hash", length=64) private String phoneHash;
    @Column(name="phone_last_four", length=4) private String phoneLastFour;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private MpesaEventStatus status = MpesaEventStatus.RECEIVED;
    @Column(name="attempt_count", nullable=false) private int attemptCount;
    @Column(name="next_attempt_at", nullable=false) private LocalDateTime nextAttemptAt = LocalDateTime.now();
    @Column(name="processing_started_at") private LocalDateTime processingStartedAt;
    @Column(name="processed_at") private LocalDateTime processedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payment_id") private Payment payment;
    @Column(name="failure_code", length=50) private String failureCode;
    @Column(name="failure_detail", length=500) private String failureDetail;
    @Column(name="received_at", nullable=false, updatable=false) private LocalDateTime receivedAt = LocalDateTime.now();
}

