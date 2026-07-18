package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.AcademicPeriodType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Fields that may be changed; omitted JSON properties remain unchanged. */
public record UpdateAcademicPeriodRequest(
        @Size(min = 1, max = 20) String code,
        @Size(min = 1, max = 100) String name,
        AcademicPeriodType periodType,
        @Positive Integer yearNumber,
        @Positive Integer periodNumber,
        @Positive Integer sequenceNumber,
        Boolean active
) {}
