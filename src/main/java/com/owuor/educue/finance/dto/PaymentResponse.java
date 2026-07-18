package com.owuor.educue.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {
    
    private Long id;
    
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    
    private java.util.UUID appliedCourseAcademicPeriodUuid;
    private String appliedAcademicPeriodName;
    
    private String receiptNumber;
    private BigDecimal amount;
    private String payerType;
    private String payerName;
    private String gatewayReference;
    private String paymentMethod;
    private LocalDateTime paidAt;
    
    private String recordedByName;
    private String status;
    private String remarks;
}
