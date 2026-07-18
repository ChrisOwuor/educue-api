package com.owuor.educue.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReverseLedgerEntryRequest(
        @NotBlank String reason,
        @NotNull LocalDate postingDate
) {}
