package com.owuor.educue.admissions.service;

import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.admissions.dto.ApplicationDocumentResponse;
import com.owuor.educue.admissions.dto.ApplicationResponse;
import com.owuor.educue.admissions.dto.CreateApplicationRequest;
import com.owuor.educue.admissions.entity.*;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import com.owuor.educue.admissions.enums.DocumentType;
import com.owuor.educue.admissions.repository.ApplicationDocumentRepository;
import com.owuor.educue.admissions.repository.ApplicationRepository;
import com.owuor.educue.admissions.repository.IntakeCourseRepository;
import com.owuor.educue.admissions.repository.IntakeRepository;
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

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final IntakeRepository intakeRepository;
    private final IntakeCourseRepository intakeCourseRepository;
    private final CourseRepository courseRepository;
    private final FileStorageService fileStorageService;

    public ApplicationResponse submit(CreateApplicationRequest request) {
        Intake intake = intakeRepository.findById(request.intakeId())
                .orElseThrow(() -> new EntityNotFoundException("Intake not found"));

        if (intake.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The application deadline for this intake has passed");
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // The actual integrity check this whole flow exists for: you
        // cannot apply to a course that isn't actually open for this
        // specific intake, even if both the intake and course
        // individually exist - someone could otherwise forge a request
        // with mismatched ids.
        if (!intakeCourseRepository.existsByIntakeIdAndCourseId(intake.getId(), course.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This course is not open for the selected intake"
            );
        }

        Application application = new Application();
        application.setIntake(intake);
        application.setCourse(course);
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
        return applicationRepository.findByIntakeId(intakeId).stream()
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
