package com.owuor.educue.graduation.dto;

import java.math.*;
import java.time.*;
import java.util.*;

public final class GraduationListDtos {
    private GraduationListDtos() {
    }

    public record Candidate(UUID enrollmentUuid, String admissionNumber,
                            String courseCode, String courseName, String academicPeriodCode, String academicPeriodName,
                            boolean finalStage, boolean readinessAssessed, boolean eligible, Long candidateId,
                            String candidateStatus) {
    }

    public record Check(String code, String label, boolean passed, String actual, String required, String message) {
    }

    public record AcademicResult(Long resultId, String periodCode, String periodName, String unitCode, String unitName,
                                 Integer creditHours, String attemptType, BigDecimal caMarks, BigDecimal examMarks,
                                 BigDecimal totalMarks, String grade, boolean passed, String status) {
    }

    public record ClearanceItem(Long checkId, String departmentName, String status, String remarks, String reviewedBy,
                                LocalDateTime reviewedAt, boolean finance) {
    }

    public record GraduationCandidateDto(UUID enrollmentUuid, Long studentId, String studentName, String admissionNumber,
                        String courseCode, String courseName, String academicPeriodCode, String academicPeriodName,
                        boolean finalStage, boolean readinessAssessed, boolean eligible, Long candidateId,
                        UUID academicYearUuid, String academicYearCode, String departmentName, String graduationName,
                        String award, BigDecimal cumulativeAverage, String classification, String remarks,
                        String status, String clearanceStage, boolean detailsConfirmed, BigDecimal graduationFee, boolean graduationFeeCharged,
                        boolean graduationFeePaid, boolean clearanceComplete, boolean financeCleared,
                        BigDecimal outstandingBalance, List<Check> checks, List<AcademicResult> academicResults,
                        List<ClearanceItem> clearanceChecks, boolean certificatePrinted,
                        LocalDateTime certificatePrintedAt, String certificateNumber) {
    }

    public record Summary(UUID uuid, UUID academicYearUuid, String academicYearCode, String departmentName, String status,
                          long entries, long eligibleEntries, LocalDateTime publishedAt, LocalDateTime submittedAt) {
    }

    public record CandidateRow(Long candidateId, String graduationName, String admissionNumber, String courseCode,
                               String courseName, BigDecimal outstandingBalance, String clearanceStage,
                               boolean financeCleared, boolean clearanceComplete, String status,
                               boolean certificatePrinted, LocalDateTime certificatePrintedAt) {
    }

    public record AssessRequest(UUID academicYearUuid) {
    }

    public record AddRequest(UUID academicYearUuid, UUID enrollmentUuid, String remarks) {
    }

    public record YearRequest(UUID academicYearUuid) {
    }

    public record UpdateDetailsRequest(String graduationName) {
    }
}
