package com.owuor.educue.students.dto;

import java.util.List;
import java.util.UUID;

public final class HodUnitRegistrationDtos {
    private HodUnitRegistrationDtos() {}
    public record IntakeOption(UUID uuid, String name, String academicYearCode, long students) {}
    public record EnrollmentOption(UUID uuid, String admissionNumber, String studentName, String courseCode,
                                   String currentPeriodCode, UUID intakeUuid, String intakeName) {}
    public record UnitOption(UUID placementUuid, String unitCode, String unitName, Integer creditHours,
                             String academicPeriodCode) {}
    public record SelectionRequest(List<UUID> enrollmentUuids) {}
    public record RegisterRequest(List<UUID> enrollmentUuids, List<UUID> placementUuids) {}
    public record RegisterResponse(int studentsProcessed, int registrationsCreated, int duplicatesSkipped,
                                   String message) {}
}
