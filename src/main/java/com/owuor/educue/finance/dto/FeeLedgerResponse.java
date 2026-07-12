package com.owuor.educue.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class FeeLedgerResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long semesterId;
    private String semesterName;
    private String transactionType;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
    private String description;
    private LocalDateTime createdAt;
}
