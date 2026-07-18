package com.owuor.educue.admissions.service;

import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import com.owuor.educue.admissions.repository.ApplicationRepository;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.repository.FeeStructureRepository;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.roles.entity.Role;
import com.owuor.educue.roles.repository.RoleRepository;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.Student;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.StudentRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Transactional
public class AdmissionApprovalService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdmissionNumberGenerator admissionNumberGenerator;
    private final FeeLedgerService feeLedgerService;
    private final FeeStructureRepository feeStructureRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Converts an approved application into:
     * <p>
     * 1. User account
     * 2. Student record
     * 3. Enrollment record
     * 4. Approved application status
     * <p>
     * ACID guarantees:
     * If ANY step fails, the entire transaction rolls back and
     * nothing is persisted.
     */
    public Application approve(Long applicationId, User approverUserId) {

        LocalDateTime now = LocalDateTime.now();

        // ---------------------------------------------------------
        // STEP 1:
        // Load the application.
        // ---------------------------------------------------------
        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Application not found"
                                ));
        var intake = application.getIntakeCourse().getIntake();
        var course = application.getIntakeCourse().getCourse();

        // ---------------------------------------------------------
        // STEP 2:
        // Prevent re-approving an already processed application.
        //
        // This protects against:
        // - double clicks
        // - browser refreshes
        // - concurrent registrar actions
        // ---------------------------------------------------------
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException(
                    "Application has already been processed"
            );
        }

        // ---------------------------------------------------------
        // STEP 3:
        // Resolve the STUDENT role.
        //
        // Every admitted learner automatically receives
        // portal access through this role.
        // ---------------------------------------------------------
        Role studentRole =
                roleRepository.findByName("STUDENT")
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Student role not configured"
                                ));

        // ---------------------------------------------------------
        // STEP 4:
        // Safety check.
        //
        // Since Student.application is OneToOne,
        // this should never happen.
        //
        // Still worth validating explicitly.
        // ---------------------------------------------------------
        if (studentRepository.existsByApplicationId(application.getId())) {
            throw new IllegalStateException(
                    "Student already exists for this application"
            );
        }

        // Resolve the first stage from the course-specific progression chain.
        // This works for semesters, terms, modules and every other period model.
        var firstCoursePeriod = courseAcademicPeriodRepository
                .findByCourseIdOrderByPosition(course.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No academic periods configured for course " + course.getName()));

        // ---------------------------------------------------------
        // STEP 7:
        // Email uniqueness check.
        //
        // We do not allow two portal users sharing
        // the same email address.
        // ---------------------------------------------------------
        if (userRepository.existsByEmail(application.getEmail())) {
            throw new IllegalStateException(
                    "A user account already exists with email "
                            + application.getEmail()
            );
        }

        // ---------------------------------------------------------
        // STEP 8:
        // Generate admission number.
        //
        // Example:
        // ICT/12345/2026
        // ---------------------------------------------------------
        String admissionNumber =
                admissionNumberGenerator.generate(
                        course.getCode()
                );

        // ---------------------------------------------------------
        // STEP 9:
        // Create portal user account.
        //
        // Initial password policy can later be replaced
        // with:
        // - password reset link
        // - activation email
        // - one-time setup token
        // ---------------------------------------------------------
        User user = new User();

        user.setFullName(application.getFullName());
        user.setEmail(application.getEmail());
        user.setPhone(application.getPhone());

        String temporaryPassword =
                admissionNumber + "@2026";

        user.setPasswordHash(
                passwordEncoder.encode(temporaryPassword)
        );
        user.setMustChangePassword(true);

        user.setRole(studentRole);
        user.setDepartment(course.getDepartment());

        user = userRepository.save(user);

        // ---------------------------------------------------------
        // STEP 10:
        // Create student record.
        //
        // This becomes the learner's permanent
        // academic profile.
        // ---------------------------------------------------------
        Student student = new Student();

        student.setApplication(application);
        student.setUser(user);
        student.setAdmissionNumber(admissionNumber);

        student.setFullName(application.getFullName());
        student.setEmail(application.getEmail());
        student.setPhone(application.getPhone());

        student = studentRepository.save(student);

        // ---------------------------------------------------------
        // STEP 11:
        // Create enrollment.
        //
        // This links:
        // Student
        // -> Course
        // -> Intake course
        // -> Initial course academic period
        // ---------------------------------------------------------
        Enrollment enrollment = new Enrollment();

        enrollment.setStudent(student);
        enrollment.setIntakeCourse(application.getIntakeCourse());
        enrollment.setCurrentCourseAcademicPeriod(firstCoursePeriod);

        enrollmentRepository.save(enrollment);

        // ---------------------------------------------------------
        // STEP 12:
        // Finalize application.
        //
        // We do this LAST so that a failed student/user/
        // enrollment creation never leaves us with an
        // application that appears approved.
        // ---------------------------------------------------------
        application.setStatus(ApplicationStatus.APPROVED);
        application.setApprovedAt(now);
        application.setReviewedAt(now);
        application.setApprovedBy(approverUserId);



        //charge the student

        FeeStructure feeStructure = feeStructureRepository
                .findByIntakeCourseIdAndCourseAcademicPeriodId(
                        application.getIntakeCourse().getId(), firstCoursePeriod.getId())
                .orElseThrow(() -> new RuntimeException("Fee structure not found."));

        feeLedgerService.billEnrollmentPeriod(student, feeStructure);


        Application savedApplication = applicationRepository.save(application);

        // Published inside the transaction but handled only AFTER_COMMIT.
        // A rollback therefore never generates a letter or sends a welcome email.
        eventPublisher.publishEvent(new StudentAdmittedEvent(
                student.getId(), application.getId(), application.getApplicationNumber(),
                admissionNumber, application.getFullName(), application.getEmail(), course.getName()
        ));

        return savedApplication;

        // ---------------------------------------------------------
        // Transaction commits here.
        //
        // If ANY exception occurred above:
        // - User is rolled back
        // - Student is rolled back
        // - Enrollment is rolled back
        // - Application remains PENDING
        // ---------------------------------------------------------
    }
}
