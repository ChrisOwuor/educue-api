package com.owuor.educue.graduation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProvisionalGraduationCandidateResponse(Long applicationId, String admissionNumber, String studentName,
                                                     String courseCode, String courseName, String departmentName,
                                                     String status, Integer requiredUnits, Integer passedUnits,
                                                     Integer failedUnits, Integer missingResults,
                                                     Integer requiredCredits, Integer earnedCredits,
                                                     Boolean clearanceComplete, BigDecimal graduationFee,
                                                     BigDecimal outstandingBalance, String ledgerDocumentNumber,
                                                     LocalDateTime appliedAt) {
}
