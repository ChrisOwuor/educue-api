package com.owuor.educue.admissions.service;

import com.owuor.educue.admissions.dto.PublicApplicationStatusResponse;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import com.owuor.educue.admissions.repository.ApplicationRepository;
import com.owuor.educue.common.storage.FileStorageService;
import com.owuor.educue.students.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class AdmissionPackService {
    private final ApplicationRepository applicationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AdmissionPackPdfService pdfService;
    private final FileStorageService storageService;

    @Transactional
    public void generate(Long applicationId, Long studentId) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));
        if (application.getStatus() != ApplicationStatus.APPROVED) return;
        if (application.getAdmissionPackKey() != null) return;
        var enrollment = enrollmentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found"));
        byte[] pdf = pdfService.generate(application, enrollment);
        String key = storageService.store(pdf, "admission-letter-" + application.getApplicationNumber() + ".pdf",
                    "application/pdf", "admission-letters/" + applicationId);
        application.setAdmissionPackKey(key);
        application.setAdmissionPackGeneratedAt(LocalDateTime.now());
        application.setAdmissionPackError(null);
        applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<PublicApplicationStatusResponse> lookup(String nationalId) {
        if (nationalId == null || nationalId.trim().length() < 5) throw new IllegalArgumentException("Enter a valid ID number");
        return applicationRepository.findByNationalIdIgnoreCaseOrderBySubmittedAtDesc(nationalId.trim()).stream().map(a -> {
            String packStatus = a.getStatus() != ApplicationStatus.APPROVED ? "NOT_AVAILABLE"
                    : a.getAdmissionPackKey() != null ? "READY" : a.getAdmissionPackError() != null ? "FAILED" : "PROCESSING";
            return new PublicApplicationStatusResponse(a.getApplicationNumber(), a.getFullName(),
                    a.getIntakeCourse().getCourse().getName(), a.getIntakeCourse().getIntake().getName(), a.getStatus(),
                    a.getSubmittedAt(), a.getApprovedAt(), packStatus,
                    a.getAdmissionPackKey() == null ? null : storageService.getAccessUrl(a.getAdmissionPackKey()));
        }).toList();
    }
}
