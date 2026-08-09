package com.owuor.educue.admissions.dto;

import com.owuor.educue.admissions.enums.ApplicationStatus;
import java.time.LocalDateTime;

public record PublicApplicationStatusResponse(String applicationNumber, String applicantName,
        String courseName, String intakeName, ApplicationStatus status, LocalDateTime submittedAt,
        LocalDateTime approvedAt, String admissionPackStatus, String admissionPackUrl) {}
