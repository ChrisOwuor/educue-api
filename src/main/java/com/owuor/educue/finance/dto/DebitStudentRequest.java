package com.owuor.educue.finance.dto;

import com.owuor.educue.finance.enums.DebitReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DebitStudentRequest(
        @NotNull Long studentId,
        UUID courseAcademicPeriodUuid,
        @NotNull DebitReason reason,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String details,
        String externalReference,
        @NotNull LocalDate postingDate
) {}
