package com.owuor.educue.graduation.dto;
import java.time.LocalDateTime; import java.util.List;
public record GraduationReadinessResponse(
 String status, boolean eligible, String courseCode, String courseName,
 boolean finalPeriodCompleted, int requiredUnits, int passedUnits, int failedUnits, int missingUnits, int missingResults,
 Integer requiredCredits, int earnedCredits, boolean clearanceComplete, int requiredClearanceDepartments, int clearedDepartments,
 LocalDateTime assessedAt, List<Blocker> blockers
) { public record Blocker(String type,String code,String message){} }
