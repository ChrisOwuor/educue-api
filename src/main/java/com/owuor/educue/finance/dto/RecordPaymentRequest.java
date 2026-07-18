package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.owuor.educue.finance.enums.PayerType;

@Getter
@Setter
public class RecordPaymentRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    private UUID appliedCourseAcademicPeriodUuid;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payer type is required")
    private PayerType payerType = PayerType.STUDENT;

    private String payerName;

    @NotBlank(message = "Gateway reference is required")
    private String gatewayReference;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Payment date is required")
    private LocalDateTime paidAt;

    private String remarks;
}
