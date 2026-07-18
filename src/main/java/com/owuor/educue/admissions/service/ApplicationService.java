package com.owuor.educue.admissions.service;

import com.owuor.educue.admissions.dto.ApplicationDocumentResponse;
import com.owuor.educue.admissions.dto.ApplicationResponse;
import com.owuor.educue.admissions.dto.CreateApplicationRequest;
import com.owuor.educue.admissions.entity.*;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import com.owuor.educue.admissions.enums.DocumentType;
import com.owuor.educue.admissions.repository.ApplicationDocumentRepository;
import com.owuor.educue.admissions.repository.ApplicationRepository;
import com.owuor.educue.admissions.repository.IntakeCourseRepository;
import com.owuor.educue.common.storage.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final IntakeCourseRepository intakeCourseRepository;
    private final FileStorageService fileStorageService;

    public ApplicationResponse submit(CreateApplicationRequest request) {
        IntakeCourse intakeCourse = intakeCourseRepository.findById(request.intakeCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Intake course not found"));
        Intake intake = intakeCourse.getIntake();

        if (intake.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The application deadline for this intake has passed");
        }

        Application application = new Application();
        application.setApplicationNumber("APP-" + UUID.randomUUID().toString().toUpperCase());
        application.setIntakeCourse(intakeCourse);
        application.setFullName(request.fullName());
        application.setEmail(request.email());
        application.setPhone(request.phone());
        application.setNationalId(request.nationalId());
        application.setDateOfBirth(request.dateOfBirth());
        application.setGuardianName(request.guardianName());
        application.setGuardianPhone(request.guardianPhone());
        application.setStatus(ApplicationStatus.PENDING);

        application = applicationRepository.save(application);

        return ApplicationResponse.from(application, List.of());
    }

    public ApplicationDocumentResponse uploadDocument(Long applicationId, DocumentType type, MultipartFile file) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was provided");
        }

        // folder = applications/{id} - keeps every applicant's documents
        // grouped, makes lifecycle/cleanup policies easy to apply per-folder
        // once this moves to S3.
        String key = fileStorageService.store(file, "applications/" + applicationId);

        ApplicationDocument doc = new ApplicationDocument();
        doc.setApplication(application);
        doc.setDocumentType(type);
        doc.setS3Key(key);
        doc.setOriginalFilename(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setFileSizeBytes(file.getSize());

        doc = documentRepository.save(doc);

        return ApplicationDocumentResponse.from(doc, fileStorageService.getAccessUrl(key));
    }

    public ApplicationResponse getById(Long id) {
        Application application = findEntity(id);
        return ApplicationResponse.from(application, getDocumentResponses(id));
    }

    public List<ApplicationResponse> getByIntake(Long intakeId) {
        return applicationRepository.findByIntakeCourseIntakeId(intakeId).stream()
                .map(app -> ApplicationResponse.from(app, getDocumentResponses(app.getId())))
                .toList();
    }

    public List<ApplicationResponse> getAll() {
        return applicationRepository.findAll().stream()
                .map(app -> ApplicationResponse.from(app, getDocumentResponses(app.getId())))
                .toList();
    }

    public ApplicationResponse approve(Long id) {
        Application application = findEntity(id);
        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);
        // NOTE: creating the actual Student + Enrollment from an approved
        // Application is a separate piece of logic, not yet built -
        // that's the Admissions->Student handoff from our lifecycle design.
        return ApplicationResponse.from(application, getDocumentResponses(id));
    }

    public ApplicationResponse reject(Long id, String notes) {
        Application application = findEntity(id);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewNotes(notes);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);
        return ApplicationResponse.from(application, getDocumentResponses(id));
    }

    private List<ApplicationDocumentResponse> getDocumentResponses(Long applicationId) {
        return documentRepository.findByApplicationId(applicationId).stream()
                .map(doc -> ApplicationDocumentResponse.from(doc, fileStorageService.getAccessUrl(doc.getS3Key())))
                .toList();
    }

    private Application findEntity(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));
    }
}
