package com.owuor.educue.admissions.service;

import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.repository.CourseCurriculumRepository;
import com.owuor.educue.academics.repository.SemesterRepository;
import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import com.owuor.educue.admissions.repository.ApplicationRepository;
import com.owuor.educue.finance.dto.ChargeStudentRequest;
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

@Service
@RequiredArgsConstructor
@Transactional
public class AdmissionApprovalService {

    private final ApplicationRepository applicationRepository;
    private final CourseCurriculumRepository curriculumRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdmissionNumberGenerator admissionNumberGenerator;
    private final FeeLedgerService feeLedgerService;
    private final FeeStructureRepository feeStructureRepository;

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

        // ---------------------------------------------------------
        // STEP 5:
        // Resolve the curriculum that NEW admissions
        // should use for this course.
        //
        // Example:
        // ICT may have:
        // - Curriculum 2024
        // - Curriculum 2026
        //
        // Only one should be marked:
        // defaultForAdmission = true
        // ---------------------------------------------------------
        CourseCurriculum curriculum =
                curriculumRepository
                        .findByCourseIdAndDefaultForAdmissionTrue(
                                application.getCourse().getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No default curriculum configured for course "
                                                + application.getCourse().getName()
                                ));


        // ---------------------------------------------------------
        // STEP 6:
        // Resolve the student's starting semester.
        //
        // Usually:
        // Year 1 Semester 1
        // ---------------------------------------------------------
        Semester firstSemester = curriculum.getFirstSemester();

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
                        application.getCourse().getCode()
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
        user.setDepartment(application.getCourse().getDepartment());

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
        // -> Curriculum
        // -> Current Semester
        // ---------------------------------------------------------
        Enrollment enrollment = new Enrollment();

        enrollment.setStudent(student);
        enrollment.setCourse(application.getCourse());
        enrollment.setCourseCurriculum(curriculum);
        enrollment.setCurrentSemester(firstSemester);

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
                .findByIntakeIdAndCourseIdAndSemesterId(
                        application.getIntake().getId(),
                        application.getCourse().getId(),
                        enrollment.getCurrentSemester().getId()
                )
                .orElseThrow(() -> new RuntimeException("Fee structure not found."));

        ChargeStudentRequest request = new ChargeStudentRequest();
        request.setStudentId(student.getId());
        request.setFeeStructureId(feeStructure.getId());

        feeLedgerService.chargeStudent(request);


        return applicationRepository.save(application);

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
