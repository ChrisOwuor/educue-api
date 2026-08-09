package com.owuor.educue.admissions.service;

import com.owuor.educue.admissions.dto.ApplicationDocumentResponse;
import com.owuor.educue.admissions.dto.ApplicationResponse;
import com.owuor.educue.admissions.dto.CreateApplicationRequest;
import com.owuor.educue.admissions.dto.ApplicationSearchResponse;
import com.owuor.educue.admissions.entity.*;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import com.owuor.educue.admissions.enums.DocumentType;
import com.owuor.educue.admissions.repository.ApplicationDocumentRepository;
import com.owuor.educue.admissions.repository.ApplicationRepository;
import com.owuor.educue.admissions.repository.IntakeCourseRepository;
import com.owuor.educue.admissions.repository.ApplicationSpecification;
import com.owuor.educue.common.storage.FileStorageService;
import com.owuor.educue.institution.enums.AcademicActivityType;
import com.owuor.educue.institution.service.AcademicActivityDeadlineService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final IntakeCourseRepository intakeCourseRepository;
    private final FileStorageService fileStorageService;
    private final AcademicActivityDeadlineService deadlineService;

    @Transactional
    public ApplicationResponse submit(CreateApplicationRequest request) {
        IntakeCourse intakeCourse = intakeCourseRepository.findById(request.intakeCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Intake course not found"));
        Intake intake = intakeCourse.getIntake();
        deadlineService.requireOpen(AcademicActivityType.APPLICATION_SUBMISSION, intake.getAcademicYear());

        if (intake.getStatus() == com.owuor.educue.admissions.enums.IntakeStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This intake is closed for applications");
        }

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

    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id) {
        Application application = findEntity(id);
        return ApplicationResponse.from(application, getDocumentResponses(id));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByIntake(Long intakeId) {
        return applicationRepository.findByIntakeCourseIntakeId(intakeId).stream()
                .map(app -> ApplicationResponse.from(app, getDocumentResponses(app.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAll() {
        return applicationRepository.findAll().stream()
                .map(app -> ApplicationResponse.from(app, getDocumentResponses(app.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationSearchResponse search(String search, ApplicationStatus status, int page, int size, String sort) {
        if (page < 0) throw new IllegalArgumentException("Page cannot be negative");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Page size must be between 1 and 100");
        String[] parts = sort == null ? new String[0] : sort.split(",");
        String property = parts.length == 0 ? "submittedAt" : switch (parts[0]) {
            case "submittedAt" -> "submittedAt";
            case "applicant" -> "fullName";
            case "course" -> "intakeCourse.course.name";
            case "intake" -> "intakeCourse.intake.name";
            case "status" -> "status";
            default -> throw new IllegalArgumentException("Unsupported application sort field: " + parts[0]);
        };
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = applicationRepository.findAll(
                Specification.where(ApplicationSpecification.search(search)).and(ApplicationSpecification.status(status)),
                PageRequest.of(page, size, Sort.by(direction, property)));
        var counts = new LinkedHashMap<String, Long>();
        for (ApplicationStatus value : ApplicationStatus.values()) counts.put(value.name(), 0L);
        for (Object[] row : applicationRepository.countByStatus()) counts.put(row[0].toString(), ((Number) row[1]).longValue());
        return new ApplicationSearchResponse(result.getContent().stream()
                .map(app -> ApplicationResponse.from(app, getDocumentResponses(app.getId()))).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize(), counts);
    }

    @Transactional
    public ApplicationResponse reject(Long id, String notes) {
        Application application = findEntity(id);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewNotes(notes);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);
        return ApplicationResponse.from(application, getDocumentResponses(id));
    }

    @Transactional
    public ApplicationResponse close(Long id, String notes) {
        Application application = findEntity(id);
        if (application.getStatus() == ApplicationStatus.APPROVED || application.getStatus() == ApplicationStatus.REJECTED)
            throw new IllegalStateException("A decided application cannot be closed");
        application.setStatus(ApplicationStatus.CLOSED);
        application.setReviewNotes(notes);
        application.setReviewedAt(LocalDateTime.now());
        return ApplicationResponse.from(applicationRepository.save(application), getDocumentResponses(id));
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
