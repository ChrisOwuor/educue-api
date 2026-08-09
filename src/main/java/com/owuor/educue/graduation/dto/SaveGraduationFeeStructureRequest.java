package com.owuor.educue.graduation.dto;

import com.owuor.educue.academics.enums.QualificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaveGraduationFeeStructureRequest(
        @NotNull QualificationType qualificationType,
        @NotNull Long intakeId,
        @NotNull @Valid List<Item> items
) {
    public record Item(@NotNull UUID feeItemUuid,
                       @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal amount) {}
}
