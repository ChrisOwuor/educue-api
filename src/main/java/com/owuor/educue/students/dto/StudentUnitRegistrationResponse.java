package com.owuor.educue.students.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class StudentUnitRegistrationResponse {

    Long registrationId;

    Long studentId;

    Long courseUnitPlacementId;

    String studentName;

    String admissionNumber;

    String course;

    String currentAcademicPeriod;

    String academicPeriod;

    String unitCode;

    String unitName;

    String attemptType;

    String status;

    String registrationOrigin;

    LocalDateTime registeredAt;
}
