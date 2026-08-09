package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.enums.UnitType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.finance.service.PeriodFeeItemService;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.students.dto.PromotionBatchResponse;
import com.owuor.educue.students.dto.PromoteStudentsRequest;
import com.owuor.educue.students.dto.StudentPromotionRowResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentPromotionService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentUnitRegistrationRepository registrationRepository;
    private final StudentResultRepository resultRepository;
    private final FeeLedgerService feeLedgerService;
    private final AcademicYearRepository academicYearRepository;
    private final PromotionJobService promotionJobService;
    private final PeriodFeeItemService periodFeeItemService;


    @Transactional
    public Page<StudentPromotionRowResponse> getPromotionManagementTable(String search, Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Page<Enrollment> enrollments = enrollmentRepository.findPromotionCandidates(normalizedSearch, pageable);
        List<StudentPromotionRowResponse> rows = enrollments.getContent().stream().map(this::buildRow).toList();
        return new PageImpl<>(rows, pageable, enrollments.getTotalElements());
    }

    /** Enqueue a promotion batch — returns immediately with a batchId for polling. */
    @Transactional
    public PromotionBatchResponse enqueuePromotion(PromoteStudentsRequest request) {
        return promotionJobService.enqueueBatch(request.getEnrollmentIds());
    }

    /**
     * Process a single enrollment promotion. Called by PromotionWorker inside its own transaction.
     * Returns null if the student was skipped (ineligible / already at final period).
     */
    @Transactional
    public String promoteSingle(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found: " + enrollmentId));

        if (!isEligibleForPromotion(enrollment)) {
            return buildIneligibilityReason(enrollment);
        }

        CourseAcademicPeriod current = enrollment.getCurrentCourseAcademicPeriod();
        CourseAcademicPeriod next = current.getNextPeriod();
        if (next == null) return "Programme completed";   // skip silently — at final period

        enrollment.setCurrentCourseAcademicPeriod(next);

        // If crossing into the next academic year
        if (next.getAcademicPeriod().getYearNumber() > current.getAcademicPeriod().getYearNumber()) {
            int nextYear = enrollment.getCurrentAcademicYear().getStartYear() + 1;
            enrollment.setCurrentAcademicYear(
                    academicYearRepository.findByStartYear(nextYear)
                            .orElseThrow(() -> new IllegalStateException("Academic year starting " + nextYear + " is not configured"))
            );
        }


        // Bill the newly selected period from the active period-fee rules.
        enrollmentRepository.save(enrollment);

        BigDecimal periodFee =
                periodFeeItemService
                        .getEffectiveTotalForEnrollment(
                                enrollment
                        );

        feeLedgerService.billEnrollmentPeriod(
                enrollment,
                periodFee
        );

        return null; // null = promoted successfully, not skipped

    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private StudentPromotionRowResponse buildRow(Enrollment enrollment) {
        CompletionSummary summary = completionSummary(enrollment);
        boolean eligible = summary.registeredUnits() > 0 && summary.coreUnits() == summary.passedCoreUnits();
        String reason = summary.registeredUnits() == 0 ? "Not Registered" : !eligible ? "Pending Core Units" : "Eligible for Promotion";

        return StudentPromotionRowResponse.builder()
                .enrollmentId(enrollment.getId())
                .studentName(enrollment.getStudent().getFullName())
                .admissionNumber(enrollment.getStudent().getAdmissionNumber())
                .courseName(enrollment.getCourse().getName())
                .currentAcademicPeriod(enrollment.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName())
                .registeredUnits(summary.registeredUnits()).coreUnits(summary.coreUnits()).passedCoreUnits(summary.passedCoreUnits())
                .eligible(eligible).status(reason).build();
    }

    private boolean isEligibleForPromotion(Enrollment enrollment) {
        CompletionSummary summary = completionSummary(enrollment);
        return summary.registeredUnits() > 0 && summary.coreUnits() == summary.passedCoreUnits();
    }

    private String buildIneligibilityReason(Enrollment enrollment) {
        List<StudentUnitRegistration> registrations =
                registrationRepository.findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodId(
                        enrollment.getId(), enrollment.getCurrentCourseAcademicPeriod().getId());
        return registrations.isEmpty() ? "Not Registered" : "Pending Core Units";
    }

    /** Registrations are attempts; academic completion is counted once per underlying unit. */
    private CompletionSummary completionSummary(Enrollment enrollment) {
        var registrations = registrationRepository.findByEnrollmentIdAndCourseUnitPlacementCourseAcademicPeriodId(
                enrollment.getId(), enrollment.getCurrentCourseAcademicPeriod().getId());
        Map<Long, StudentUnitRegistration> distinctUnits = registrations.stream().collect(Collectors.toMap(
                r -> r.getCourseUnitPlacement().getUnit().getId(), r -> r, (first, ignored) -> first));
        Set<Long> coreUnitIds = distinctUnits.values().stream()
                .filter(r -> r.getCourseUnitPlacement().getUnitType() == UnitType.CORE)
                .map(r -> r.getCourseUnitPlacement().getUnit().getId()).collect(Collectors.toSet());
        Set<Long> passedUnitIds = resultRepository.findByStudentUnitRegistrationEnrollmentId(enrollment.getId()).stream()
                .filter(StudentResult::isPassed)
                .filter(r -> r.getStatus() == ResultStatus.APPROVED || r.getStatus() == ResultStatus.RELEASED)
                .map(r -> r.getStudentUnitRegistration().getCourseUnitPlacement().getUnit().getId())
                .collect(Collectors.toSet());
        int passedCore = (int) coreUnitIds.stream().filter(passedUnitIds::contains).count();
        return new CompletionSummary(distinctUnits.size(), coreUnitIds.size(), passedCore);
    }

    private record CompletionSummary(int registeredUnits, int coreUnits, int passedCoreUnits) {}
}
