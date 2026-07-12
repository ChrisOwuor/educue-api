package com.owuor.educue.students.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class StudentUnitRegistrationResponse {

    Long registrationId;

    Long studentId;

    String studentName;

    String admissionNumber;

    String course;

    String currentSemester;

    String curriculum;

    String semester;

    String unitCode;

    String unitName;

    String attemptType;

    String status;

    LocalDateTime registeredAt;
}
