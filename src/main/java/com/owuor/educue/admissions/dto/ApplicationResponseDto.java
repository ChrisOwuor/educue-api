package com.owuor.educue.admissions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

public record ApplicationResponseDto(
        String fullName,
        String status
) {
}
