package com.owuor.educue.admissions.dto;

import com.owuor.educue.admissions.entity.ApplicationDocument;

public record ApplicationDocumentResponse(
        Long id,
        String documentType,
        String originalFilename,
        String accessUrl
) {
    public static ApplicationDocumentResponse from(ApplicationDocument doc, String accessUrl) {
        return new ApplicationDocumentResponse(
                doc.getId(),
                doc.getDocumentType().name(),
                doc.getOriginalFilename(),
                accessUrl
        );
    }
}
