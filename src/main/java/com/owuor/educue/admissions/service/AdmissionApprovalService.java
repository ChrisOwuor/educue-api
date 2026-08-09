package com.owuor.educue.admissions.service;

import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import com.owuor.educue.admissions.repository.ApplicationRepository;
import com.owuor.educue.students.dto.CreateStudentEnrollmentRequest;
import com.owuor.educue.students.service.StudentOnboardingService;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdmissionApprovalService {
    private final ApplicationRepository applicationRepository;
    private final AdmissionNumberGenerator admissionNumberGenerator;
    private final CourseAcademicPeriodRepository coursePeriodRepository;
    private final StudentOnboardingService onboardingService;
    private final AdmissionJobService admissionJobService;

    public void approve(Long applicationId, User approver) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));
        if (application.getStatus() != ApplicationStatus.PENDING)
            throw new IllegalStateException("Application has already been processed");

        var offering = application.getIntakeCourse();
        var intake = offering.getIntake();
        var course = offering.getCourse();
        var firstPeriod = coursePeriodRepository.findByCourseIdOrderByPosition(course.getId()).stream()
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "No academic periods configured for course " + course.getName()));
        String admissionNumber = admissionNumberGenerator.generate(course.getCode());

        var created = onboardingService.create(
                new CreateStudentEnrollmentRequest(
                        admissionNumber,
                        application.getFullName(),
                        application.getEmail(),
                        application.getPhone(),
                        application.getNationalId(),
                        application.getDateOfBirth(),
                        application.getGuardianName(),
                        application.getGuardianPhone(),

                        course.getId(),
                        intake.getId(),

                        intake.getAcademicYear().getUuid(),
                        intake.getAcademicYear().getUuid(),
                        firstPeriod.getUuid(),

                        intake.getStartDate(),
                        false,
                        null,
                        null,
                        null,
                        null
                )
        );

        LocalDateTime now = LocalDateTime.now();
        application.setStatus(ApplicationStatus.APPROVED);
        application.setApprovedAt(now);
        application.setReviewedAt(now);
        application.setApprovedBy(approver);
        applicationRepository.save(application);

        admissionJobService.enqueue(application.getId(), created.studentId());
    }
}
