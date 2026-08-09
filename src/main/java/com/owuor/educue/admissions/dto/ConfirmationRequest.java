package com.owuor.educue.admissions.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmationRequest(@NotNull Boolean confirmed) {}
