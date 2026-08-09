package com.owuor.educue.graduation.dto;

import com.owuor.educue.results.dto.MyAcademicResultsResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GraduationApplicationDetailResponse(

        Long applicationId,
        String status,
        LocalDateTime appliedAt,

        Student student,
        Course course,

        String awardTitle,
        String qualificationType,

        GraduationReadinessResponse readiness,

        Finance finance,

        Academic academic,

        Approval approval,

        Actions actions

) {

    public record Student(
            Long enrollmentId,
            String admissionNumber,
            String fullName,
            String email,
            String phone
    ) {
    }

    public record Course(
            Long courseId,
            String courseCode,
            String courseName,
            String departmentName
    ) {
    }

    public record Finance(
            BigDecimal graduationFee,
            BigDecimal outstandingBalance,
            boolean fullyPaid,
            String ledgerDocumentNumber,
            List<FeeItem> feeItems
    ) {
    }

    public record FeeItem(
            UUID feeItemUuid,
            String code,
            String name,
            BigDecimal amount,
            Integer displayOrder
    ) {
    }

    public record Academic(
            BigDecimal currentCumulativeAverage,
            BigDecimal finalCumulativeAverage,
            String awardClassification,
            List<MyAcademicResultsResponse.AcademicPeriodResult> periods
    ) {
    }

    public record Approval(
            String approvedBy,
            LocalDateTime approvedAt
    ) {
    }

    public record Actions(
            boolean canApprove,
            boolean canReject
    ) {
    }
}
