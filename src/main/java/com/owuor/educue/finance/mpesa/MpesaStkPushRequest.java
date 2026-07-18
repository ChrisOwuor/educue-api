package com.owuor.educue.finance.mpesa;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.students.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="mpesa_stk_push_requests")
@Getter @Setter @NoArgsConstructor
public class MpesaStkPushRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, updatable=false) private UUID uuid = UUID.randomUUID();
    @Column(name="idempotency_key", nullable=false, unique=true, updatable=false) private UUID idempotencyKey;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id", nullable=false) private Student student;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="course_academic_period_id") private CourseAcademicPeriod courseAcademicPeriod;
    @Column(name="account_reference", nullable=false, length=80) private String accountReference;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Column(name="phone_hash", nullable=false, length=64) private String phoneHash;
    @Column(name="phone_last_four", nullable=false, length=4) private String phoneLastFour;
    @Column(name="merchant_request_id", length=100) private String merchantRequestId;
    @Column(name="checkout_request_id", unique=true, length=100) private String checkoutRequestId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private StkPushStatus status = StkPushStatus.REQUESTED;
    @Column(name="response_code", length=30) private String responseCode;
    @Column(name="response_description", length=255) private String responseDescription;
    @Column(name="customer_message", length=255) private String customerMessage;
    @Column(name="result_code") private Integer resultCode;
    @Column(name="result_description", length=500) private String resultDescription;
    @Column(name="mpesa_receipt_number", length=40) private String mpesaReceiptNumber;
    @Column(name="requested_at", nullable=false, updatable=false) private LocalDateTime requestedAt = LocalDateTime.now();
    @Column(name="callback_received_at") private LocalDateTime callbackReceivedAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
}

