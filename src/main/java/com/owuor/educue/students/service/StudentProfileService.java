package com.owuor.educue.students.service;

import com.owuor.educue.students.dto.StudentProfileResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.dto.FeeStructureResponse;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.finance.service.FeeStructureService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentProfileService {

    private final EnrollmentRepository enrollmentRepository;
    private final FeeStructureService feeStructureService;
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
        var course = enrollment.getIntakeCourse().getCourse();

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
                .build();
    }

    public FeeStructureResponse getMyFeeStructure(Long userId) {
        return feeStructureService.getStudentFeeStructure(userId);
    }

    public java.util.List<FeeStructureResponse> getAllMyFeeStructures(Long userId) {
        return feeStructureService.getAllStudentFeeStructures(userId);
    }

    public java.util.List<FeeLedgerResponse> getMyLedger(Long userId) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return feeLedgerService.getStudentLedger(enrollment.getStudent().getId());
    }
}
