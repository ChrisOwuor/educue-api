package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.enums.UnitType;
import com.owuor.educue.academics.enums.RegistrationStatus;
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
                        .findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodIdAndStatus(
                                enrollment.getId(),
                                enrollment.getCurrentCourseAcademicPeriod().getId(),
                                RegistrationStatus.ACTIVE
                        );

        int registeredUnits = registrations.size();

        int coreUnits = (int) registrations.stream()
                .map(StudentUnitRegistration::getCourseUnitPlacement)
                .filter(placement -> placement.getUnitType() == UnitType.CORE)
                .count();

        int passedCoreUnits = 0;

        for (StudentUnitRegistration registration : registrations) {

            if (registration.getCourseUnitPlacement().getUnitType() != UnitType.CORE) {
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

                passedCoreUnits++;
            }
        }

        boolean eligible =
                registeredUnits > 0
                && coreUnits == passedCoreUnits;

        String reason;

        if (registeredUnits == 0) {
            reason = "Not Registered";
        } else if (!eligible) {
            reason = "Pending Core Units";
        } else {
            reason = "Eligible for Promotion";
        }

        return StudentPromotionRowResponse.builder()
                .enrollmentId(enrollment.getId())
                .studentName(enrollment.getStudent().getFullName())
                .admissionNumber(enrollment.getStudent().getAdmissionNumber())
                .currentAcademicPeriod(enrollment.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName())
                .registeredUnits(registeredUnits)
                .coreUnits(coreUnits)
                .passedCoreUnits(passedCoreUnits)
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

            CourseAcademicPeriod current = enrollment.getCurrentCourseAcademicPeriod();
            CourseAcademicPeriod next = current.getNextPeriod();

            if (next == null) {
                continue; // Student has completed the programme
            }

            enrollment.setCurrentCourseAcademicPeriod(next);

            FeeStructure feeStructure = feeStructureRepository
                    .findByIntakeCourseIdAndCourseAcademicPeriodId(
                            enrollment.getIntakeCourse().getId(), next.getId())
                    .orElseThrow(() ->
                            new RuntimeException("Fee structure not found."));

            feeLedgerService.billEnrollmentPeriod(enrollment.getStudent(), feeStructure);
        }
    }

    private boolean isEligibleForPromotion(Enrollment enrollment) {

        List<StudentUnitRegistration> registrations =
                registrationRepository.findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodId(
                        enrollment.getId(),
                        enrollment.getCurrentCourseAcademicPeriod().getId()
                );

        if (registrations.isEmpty()) {
            return false;
        }

        long coreUnits =
                registrations.stream()
                        .filter(r -> r.getCourseUnitPlacement().getUnitType() == UnitType.CORE)
                        .count();

        long passed =
                registrations.stream()
                        .filter(r -> r.getCourseUnitPlacement().getUnitType() == UnitType.CORE)
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

        return coreUnits == passed;
    }
}
