package com.owuor.educue.graduation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GraduationApplicationListItemResponse(

        Long applicationId,
        Long enrollmentId,

        String admissionNumber,
        String studentName,
        String studentEmail,

        String courseCode,
        String courseName,
        String departmentName,

        /*
         * Application-time snapshots.
         */
        String awardTitle,
        String qualificationType,

        String status,

        /*
         * Readiness snapshot.
         */
        Integer requiredUnits,
        Integer passedUnits,
        Integer failedUnits,
        Integer missingResults,

        Integer requiredCredits,
        Integer earnedCredits,

        Boolean clearanceComplete,
        LocalDateTime eligibilityAssessedAt,

        /*
         * Finance information.
         */
        BigDecimal graduationFee,
        BigDecimal studentOutstandingBalance,
        String ledgerDocumentNumber,

        /*
         * These remain null until academic approval.
         */
        BigDecimal finalCumulativeAverage,
        String awardClassification,

        String academicApprovedBy,
        LocalDateTime academicApprovedAt,

        LocalDateTime appliedAt

) {
}
