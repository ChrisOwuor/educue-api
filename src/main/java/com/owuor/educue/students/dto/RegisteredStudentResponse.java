package com.owuor.educue.students.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RegisteredStudentResponse {

    private Long registrationId;

    private Long studentId;

    private String admissionNumber;

    private String studentName;

    private String attemptType;

    private String status;

    private LocalDateTime registeredAt;
}
