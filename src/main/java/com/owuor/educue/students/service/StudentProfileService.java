package com.owuor.educue.students.service;

import com.owuor.educue.students.dto.StudentProfileResponse;
import com.owuor.educue.students.dto.CompleteStudentProfileRequest;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.StudentRepository;
import com.owuor.educue.users.repository.UserRepository;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.service.FeeLedgerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentProfileService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final FeeLedgerService feeLedgerService;

    public StudentProfileResponse getMyProfile(Long userId) {

        Enrollment enrollment =
                enrollmentRepository.findByStudentUserId(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Student profile not found"
                                ));

        var coursePeriod = enrollment.getCurrentCourseAcademicPeriod();
        var period = coursePeriod.getAcademicPeriod();
        var course = enrollment.getCourse();

        return StudentProfileResponse.builder()
                .studentId(
                        enrollment.getStudent().getId()
                )
                .admissionNumber(
                        enrollment.getStudent().getAdmissionNumber()
                )
                .fullName(
                        enrollment.getStudent().getFullName()
                )
                .email(
                        enrollment.getStudent().getEmail()
                )
                .phone(
                        enrollment.getStudent().getPhone()
                )
                .courseCode(
                        course.getCode()
                )
                .courseName(
                        course.getName()
                )
                .intakeId(enrollment.getIntake().getId())
                .intakeName(enrollment.getIntake().getName())
                .enrolledAcademicYearUuid(enrollment.getEnrolledAcademicYear().getUuid())
                .enrolledAcademicYearCode(enrollment.getEnrolledAcademicYear().getCode())
                .currentAcademicYearUuid(enrollment.getCurrentAcademicYear().getUuid())
                .currentAcademicYearCode(enrollment.getCurrentAcademicYear().getCode())
                .currentYear(
                        period.getYearNumber()
                )
                .courseAcademicPeriodUuid(coursePeriod.getUuid())
                .academicPeriodCode(period.getCode())
                .academicPeriodName(period.getName())
                .academicPeriodType(period.getPeriodType().name())
                .academicPeriodNumber(period.getPeriodNumber())
                .enrollmentStatus(
                        enrollment.getStatus().name()
                )
                .admissionDate(
                        enrollment.getAdmissionDate()
                )
                .profileCompletionRequired(enrollment.getStudent().isProfileCompletionRequired())
                .build();
    }

    @Transactional
    public StudentProfileResponse completeMyProfile(Long userId, CompleteStudentProfileRequest request) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId))
            throw new IllegalArgumentException("Portal email already exists");
        var student = enrollment.getStudent();
        var user = student.getUser();
        user.setEmail(email);
        user.setPhone(clean(request.phone()));
        student.setEmail(email);
        student.setPhone(clean(request.phone()));
        student.setNationalId(clean(request.nationalId()));
        student.setDateOfBirth(request.dateOfBirth());
        student.setGuardianName(clean(request.guardianName()));
        student.setGuardianPhone(clean(request.guardianPhone()));
        student.setProfileCompletionRequired(false);
        userRepository.save(user);
        studentRepository.save(student);
        return getMyProfile(userId);
    }

    public java.util.List<FeeLedgerResponse> getMyLedger(Long userId) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return feeLedgerService.getStudentLedger(enrollment.getStudent().getId());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
