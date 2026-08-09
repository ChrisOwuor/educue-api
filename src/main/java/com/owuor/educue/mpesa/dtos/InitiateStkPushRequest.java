package com.owuor.educue.mpesa.dtos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
public record InitiateStkPushRequest(@NotNull @DecimalMin("1.00") BigDecimal amount,
                                     @NotBlank String phoneNumber,
                                     @NotNull UUID idempotencyKey) {}


