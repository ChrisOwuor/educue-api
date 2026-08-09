package com.owuor.educue.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Getter
@Builder
public class FeeLedgerResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private java.util.UUID courseAcademicPeriodUuid;
    private String academicPeriodName;
    private String academicPeriodCode;
    private java.util.UUID academicPeriodUuid;
    private String transactionType;
    private LocalDate postingDate;
    private String documentNumber;
    private String externalReference;
    private String status;
    private Long reversalOfId;
    private Long paymentId;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
    private String description;
    private LocalDateTime createdAt;
}
