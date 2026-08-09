package com.owuor.educue.finance.dto;

import com.owuor.educue.finance.entity.PeriodFeeItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PeriodFeeItemResponse(
        Long id,

        UUID courseAcademicPeriodUuid,
        Long courseId,
        String courseCode,
        String courseName,

        UUID academicPeriodUuid,
        String academicPeriodCode,
        String academicPeriodName,

        UUID feeItemUuid,
        String feeItemCode,
        String feeItemName,
        String feeItemCategory,

        BigDecimal amount,

        Long effectiveFromIntakeId,
        String effectiveFromIntakeName,
        Long effectiveFromIntakeSequence,

        Long effectiveToIntakeId,
        String effectiveToIntakeName,
        Long effectiveToIntakeSequence,

        boolean mandatory,
        int displayOrder,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PeriodFeeItemResponse from(PeriodFeeItem item) {
        var courseAcademicPeriod = item.getCourseAcademicPeriod();
        var course = courseAcademicPeriod.getCourse();
        var academicPeriod = courseAcademicPeriod.getAcademicPeriod();
        var feeItem = item.getFeeItem();
        var fromIntake = item.getEffectiveFromIntake();
        var toIntake = item.getEffectiveToIntake();

        return new PeriodFeeItemResponse(
                item.getId(),

                courseAcademicPeriod.getUuid(),
                course.getId(),
                course.getCode(),
                course.getName(),

                academicPeriod.getUuid(),
                academicPeriod.getCode(),
                academicPeriod.getName(),

                feeItem.getUuid(),
                feeItem.getCode(),
                feeItem.getName(),
                feeItem.getCategory(),

                item.getAmount(),

                fromIntake.getId(),
                fromIntake.getName(),
                fromIntake.getSequenceNumber(),

                toIntake != null ? toIntake.getId() : null,
                toIntake != null ? toIntake.getName() : null,
                toIntake != null ? toIntake.getSequenceNumber() : null,

                item.isMandatory(),
                item.getDisplayOrder(),

                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
