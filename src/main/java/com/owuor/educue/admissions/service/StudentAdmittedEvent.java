package com.owuor.educue.admissions.service;

public record StudentAdmittedEvent(
        Long studentId,
        Long applicationId,
        String applicationNumber,
        String admissionNumber,
        String studentName,
        String email,
        String courseName
) {}
