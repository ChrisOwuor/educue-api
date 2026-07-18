package com.owuor.educue.admissions.dto;

import com.owuor.educue.admissions.entity.Application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record ApplicationResponse(
        Long id,
        String applicationNumber,
        Long intakeCourseId,
        String intakeName,
        String courseName,
        String fullName,
        String email,
        String phone,
        String nationalId,
        LocalDate dateOfBirth,
        String status,
        String reviewNotes,
        LocalDateTime submittedAt,
        List<ApplicationDocumentResponse> documents
) {
    public static ApplicationResponse from(Application app, List<ApplicationDocumentResponse> documents) {
        return new ApplicationResponse(
                app.getId(),
                app.getApplicationNumber(),
                app.getIntakeCourse().getId(),
                app.getIntakeCourse().getIntake().getName(),
                app.getIntakeCourse().getCourse().getName(),
                app.getFullName(),
                app.getEmail(),
                app.getPhone(),
                app.getNationalId(),
                app.getDateOfBirth(),
                app.getStatus().name(),
                app.getReviewNotes(),
                app.getSubmittedAt(),
                new ArrayList<>(documents)
        );
    }
}
