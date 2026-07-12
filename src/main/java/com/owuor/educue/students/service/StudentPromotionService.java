package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.finance.dto.ChargeStudentRequest;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.repository.FeeStructureRepository;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.students.dto.PromoteStudentsRequest;
import com.owuor.educue.students.dto.StudentPromotionRowResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.enums.EnrollmentStatus;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentPromotionService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentUnitRegistrationRepository registrationRepository;
    private final StudentResultRepository resultRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final FeeLedgerService feeLedgerService;


    public Page<StudentPromotionRowResponse> getPromotionManagementTable(
            String search,
            Pageable pageable
    ) {

        Page<Enrollment> enrollments =
                enrollmentRepository.findPromotionCandidates(
                        search,
                        pageable
                );

        List<StudentPromotionRowResponse> rows =
                enrollments.getContent()
                        .stream()
                        .map(this::buildRow)
                        .toList();

        return new PageImpl<>(
                rows,
                pageable,
                enrollments.getTotalElements()
        );
    }

    private StudentPromotionRowResponse buildRow(Enrollment enrollment) {

        List<StudentUnitRegistration> registrations =
                registrationRepository
                        .findByEnrollmentIdAndSemesterUnitSemesterIdAndStatus(
                                enrollment.getId(),
                                enrollment.getCurrentSemester().getId(),
                                RegistrationStatus.ACTIVE
                        );

        int registeredUnits = registrations.size();

        int mandatoryUnits = (int) registrations.stream()
                .map(StudentUnitRegistration::getSemesterUnit)
                .filter(SemesterUnit::isMandatory)
                .count();

        int passedMandatoryUnits = 0;

        for (StudentUnitRegistration registration : registrations) {

            if (!registration.getSemesterUnit().isMandatory()) {
                continue;
            }

            StudentResult result =
                    resultRepository
                            .findByStudentUnitRegistrationId(
                                    registration.getId()
                            )
                            .orElse(null);

            if (result != null
                && result.isPassed()
                && result.getStatus() == ResultStatus.APPROVED) {

                passedMandatoryUnits++;
            }
        }

        boolean eligible =
                registeredUnits > 0
                && mandatoryUnits == passedMandatoryUnits;

        String reason;

        if (registeredUnits == 0) {
            reason = "Not Registered";
        } else if (!eligible) {
            reason = "Pending Mandatory Units";
        } else {
            reason = "Eligible for Promotion";
        }

        return StudentPromotionRowResponse.builder()
                .enrollmentId(enrollment.getId())
                .studentName(enrollment.getStudent().getFullName())
                .admissionNumber(enrollment.getStudent().getAdmissionNumber())
                .currentSemester(enrollment.getCurrentSemester().getName())
                .registeredUnits(registeredUnits)
                .mandatoryUnits(mandatoryUnits)
                .passedMandatoryUnits(passedMandatoryUnits)
                .eligible(eligible)
                .status(reason)
                .build();
    }

    @Transactional
    public void promoteStudents(PromoteStudentsRequest request) {

        List<Enrollment> enrollments =
                enrollmentRepository.findAllById(request.getEnrollmentIds());


        for (Enrollment enrollment : enrollments) {

            if (!isEligibleForPromotion(enrollment)) {
                continue;
            }

            Semester current = enrollment.getCurrentSemester();
            Semester next = current.getNextSemester();

            if (next == null) {
                continue; // Student has completed the programme
            }

            enrollment.setCurrentSemester(next);

            Application application = enrollment.getStudent().getApplication();

            FeeStructure feeStructure = feeStructureRepository
                    .findByIntakeIdAndCourseIdAndSemesterId(
                            application.getIntake().getId(),
                            enrollment.getCourse().getId(),
                            next.getId()
                    )
                    .orElseThrow(() ->
                            new RuntimeException("Fee structure not found."));

            ChargeStudentRequest chargeStudentRequest = new ChargeStudentRequest();
            chargeStudentRequest.setStudentId(enrollment.getStudent().getId());
            chargeStudentRequest.setFeeStructureId(feeStructure.getId());

            feeLedgerService.chargeStudent(chargeStudentRequest);
        }
    }

    private boolean isEligibleForPromotion(Enrollment enrollment) {

        List<StudentUnitRegistration> registrations =
                registrationRepository.findByEnrollmentIdAndSemesterUnitSemesterId(
                        enrollment.getId(),
                        enrollment.getCurrentSemester().getId()
                );

        if (registrations.isEmpty()) {
            return false;
        }

        long mandatoryUnits =
                registrations.stream()
                        .filter(r -> r.getSemesterUnit().isMandatory())
                        .count();

        long passed =
                registrations.stream()
                        .filter(r -> r.getSemesterUnit().isMandatory())
                        .filter(r -> {

                            StudentResult result =
                                    resultRepository
                                            .findByStudentUnitRegistrationId(r.getId())
                                            .orElse(null);

                            return result != null
                                   && result.getStatus() == ResultStatus.APPROVED
                                   && result.isPassed();
                        })
                        .count();

        return mandatoryUnits == passed;
    }
}
