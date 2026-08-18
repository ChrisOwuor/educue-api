package com.owuor.educue.students.service;

import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.finance.service.PeriodFeeItemService;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.institution.enums.AcademicActivityType;
import com.owuor.educue.institution.service.AcademicActivityDeadlineService;
import com.owuor.educue.roles.repository.RoleRepository;
import com.owuor.educue.students.dto.CreateStudentEnrollmentRequest;
import com.owuor.educue.students.dto.CreatedStudentEnrollmentResponse;
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

import java.util.Locale;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentOnboardingService {
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final IntakeRepository intakeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CourseAcademicPeriodRepository coursePeriodRepository;
    private final PasswordEncoder passwordEncoder;
    private final FeeLedgerService feeLedgerService;
    private final AcademicActivityDeadlineService deadlineService;
    private final PeriodFeeItemService periodFeeItemService;
    @Transactional
    public CreatedStudentEnrollmentResponse create(CreateStudentEnrollmentRequest request) {
        validateFinancialPosition(request);
        String admissionNumber = request.admissionNumber().trim().toUpperCase(Locale.ROOT);
        boolean requiresProfileCompletion = Boolean.TRUE.equals(request.migrated());
        String email = request.email() == null || request.email().isBlank()
                ? migrationPlaceholderEmail(admissionNumber)
                : request.email().trim().toLowerCase(Locale.ROOT);
        if (studentRepository.findByAdmissionNumberIgnoreCase(admissionNumber).isPresent())
            throw new IllegalArgumentException("Admission number already exists");
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Portal email already exists");

        var course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        var intake = intakeRepository.findById(request.intakeId())
                .orElseThrow(() -> new EntityNotFoundException("Intake not found"));
        var enrolledYear = academicYearRepository
                .findByUuid(request.enrolledAcademicYearUuid())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Enrolled academic year not found: "
                        + request.enrolledAcademicYearUuid()
                ));

        var currentYear = academicYearRepository
                .findByUuid(request.currentAcademicYearUuid())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Current academic year not found: "
                        + request.currentAcademicYearUuid()
                ));

        var coursePeriod = coursePeriodRepository
                .findByUuid(request.currentCourseAcademicPeriodUuid())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course academic period not found: "
                        + request.currentCourseAcademicPeriodUuid()
                ));

        if (!intake.getAcademicYear().getId().equals(enrolledYear.getId()))
            throw new IllegalArgumentException("The intake must belong to the student's enrolled academic year");
        if (!coursePeriod.getCourse().getId().equals(course.getId()))
            throw new IllegalArgumentException("Current course period does not belong to the selected course");
        if (currentYear.getStartYear() < enrolledYear.getStartYear())
            throw new IllegalArgumentException("Current academic year cannot be before the enrolled academic year");

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(clean(request.phone()));
        user.setRole(roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new EntityNotFoundException("Student role not configured")));
        user.setDepartment(course.getDepartment());
        user.setPasswordHash(passwordEncoder.encode(admissionNumber));
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setAdmissionNumber(admissionNumber);
        student.setFullName(request.fullName().trim());
        student.setEmail(email);
        student.setPhone(clean(request.phone()));
        student.setNationalId(clean(request.nationalId()));
        student.setDateOfBirth(request.dateOfBirth());
        student.setGuardianName(clean(request.guardianName()));
        student.setGuardianPhone(clean(request.guardianPhone()));
        student.setProfileCompletionRequired(requiresProfileCompletion);
        student = studentRepository.save(student);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setDepartment(course.getDepartment());
        enrollment.setIntake(intake);
        enrollment.setEnrolledAcademicYear(enrolledYear);
        enrollment.setCurrentAcademicYear(currentYear);
        enrollment.setCurrentCourseAcademicPeriod(coursePeriod);
        enrollment.setAdmissionDate(request.admissionDate());
        enrollment = enrollmentRepository.save(enrollment);


        if (Boolean.TRUE.equals(request.migrated())) {
            BigDecimal signedBalance =
                    value(request.openingDebit())
                            .subtract(
                                    value(request.openingCredit())
                            );

            feeLedgerService.postMigrationOpeningBalance(
                    student,
                    signedBalance,
                    request.openingBalanceDate(),
                    request.legacyReference()
            );
        } else {
            BigDecimal periodFee =
                    periodFeeItemService
                            .getEffectiveTotalForEnrollment(
                                    enrollment
                            );

            feeLedgerService.billEnrollmentPeriod(
                    enrollment,
                    periodFee
            );
        }
        return new CreatedStudentEnrollmentResponse(student.getId(), enrollment.getId(), user.getId(),
                admissionNumber, student.getFullName(), admissionNumber);
    }

    @Transactional
    public List<CreatedStudentEnrollmentResponse> createBulk(List<CreateStudentEnrollmentRequest> requests) {
        if (requests == null || requests.isEmpty())
            throw new IllegalArgumentException("Bulk import must contain at least one student");
        if (requests.size() > 500) throw new IllegalArgumentException("A bulk import cannot exceed 500 students");
        return requests.stream().map(this::create).toList();
    }

    private void validateFinancialPosition(CreateStudentEnrollmentRequest request) {
        BigDecimal debit = value(request.openingDebit());
        BigDecimal credit = value(request.openingCredit());
        if (debit.signum() > 0 && credit.signum() > 0)
            throw new IllegalArgumentException("Enter either an opening debit or an opening credit, not both");
        if (!Boolean.TRUE.equals(request.migrated()) && (debit.signum() > 0 || credit.signum() > 0))
            throw new IllegalArgumentException("Opening debit or credit is only allowed for a migrated student");
        if ((debit.signum() > 0 || credit.signum() > 0) && request.openingBalanceDate() == null)
            throw new IllegalArgumentException("Opening balance date is required when importing a balance");
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String migrationPlaceholderEmail(String admissionNumber) {
        return "migration-" + admissionNumber.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-")
                + "@placeholder.educue.local";
    }
}
