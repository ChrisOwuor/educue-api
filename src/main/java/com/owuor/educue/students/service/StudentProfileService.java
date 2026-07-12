package com.owuor.educue.students.service;

import com.owuor.educue.students.dto.StudentProfileResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.dto.FeeStructureResponse;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.repository.FeeStructureRepository;
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
    private final FeeStructureRepository feeStructureRepository;
    private final FeeStructureService feeStructureService;
    private final FeeLedgerService feeLedgerService;

    public StudentProfileResponse getMyProfile(Long userId) {

        Enrollment enrollment =
                enrollmentRepository.findByStudentUserId(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Student profile not found"
                                ));

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
                        enrollment.getCourse().getCode()
                )
                .courseName(
                        enrollment.getCourse().getName()
                )
                .curriculumName(
                        enrollment.getCourseCurriculum().getName()
                )
                .currentYear(
                        enrollment.getCurrentSemester().getYearNumber()
                )
                .currentSemester(
                        enrollment.getCurrentSemester().getSemesterNumber()
                )
                .currentSemesterName(
                        enrollment.getCurrentSemester().getName()
                )
                .enrollmentStatus(
                        enrollment.getStatus().name()
                )
                .admissionDate(
                        enrollment.getAdmissionDate()
                )
                .build();
    }

    public FeeStructureResponse getMyFeeStructure(Long userId) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));

        Long intakeId = enrollment.getStudent().getApplication().getIntake().getId();
        Long courseId = enrollment.getCourse().getId();
        Long semesterId = enrollment.getCurrentSemester().getId();

        FeeStructure structure = feeStructureRepository.findByIntakeIdAndCourseIdAndSemesterId(intakeId, courseId, semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Fee structure not found for this student's intake, course, and semester."));

        return feeStructureService.toResponse(structure);
    }

    public java.util.List<FeeLedgerResponse> getMyLedger(Long userId) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return feeLedgerService.getStudentLedger(enrollment.getStudent().getId());
    }
}
