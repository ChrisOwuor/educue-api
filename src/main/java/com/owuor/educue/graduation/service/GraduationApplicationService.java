package com.owuor.educue.graduation.service;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.finance.entity.GraduationFeeItem;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.finance.entity.FeeLedger;
import com.owuor.educue.finance.repository.FeeLedgerRepository;
import com.owuor.educue.common.report.ProfessionalPdfService;
import com.owuor.educue.graduation.dto.*;
import com.owuor.educue.graduation.entity.GraduationApplication;
import com.owuor.educue.graduation.entity.GraduationApplicationFeeItem;
import com.owuor.educue.graduation.enums.GraduationApplicationStatus;
import com.owuor.educue.graduation.repository.GraduationApplicationRepository;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.enums.EnrollmentStatus;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GraduationApplicationService {
    private final EnrollmentRepository enrollmentRepository;
    private final GraduationApplicationRepository applicationRepository;
    private final GraduationFeeService graduationFeeService;
    private final FeeLedgerService feeLedgerService;
    private final FeeLedgerRepository feeLedgerRepository;
    private final GraduationReadinessService readinessService;
    private final ProfessionalPdfService pdfService;

    @Transactional(readOnly = true)
    public GraduationReadinessResponse readiness(User user) { return readinessService.assess(enrollment(user)); }

    @Transactional(readOnly = true)
    public GraduationFeeStructureResponse preview(User user) {
        Enrollment enrollment = enrollment(user);
        requireEligible(enrollment);
        return graduationFeeService.getStructure(enrollment.getCourse().getQualificationType(), enrollment.getIntake().getId());
    }

    @Transactional
    public GraduationApplicationResponse apply(
            User user
    ) {
        /*
         * Lock the enrollment so simultaneous requests cannot
         * create duplicate applications or graduation bills.
         */
        Enrollment enrollment =
                enrollmentRepository
                        .findByStudentUserIdForUpdate(
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Student enrollment not found"
                                )
                        );

        /*
         * Idempotency:
         *
         * A double-click or repeated HTTP request must return
         * the existing application instead of charging twice.
         */
        var existingApplication =
                applicationRepository
                        .findFirstByEnrollmentIdOrderByGraduationListAcademicYearStartDateDesc(
                                enrollment.getId()
                        );

        if (existingApplication.isPresent()) {
            return response(
                    existingApplication.get()
            );
        }

        /*
         * Confirm:
         *
         * - final academic period completed
         * - all required units passed
         * - no missing results
         * - required credits earned
         * - institutional clearance completed
         */
        GraduationReadinessResponse readiness =
                requireEligible(enrollment);

        /*
         * Official award details are obtained from the course.
         * They must never be supplied by the frontend.
         */
        String awardTitle =
                requireAwardTitle(enrollment);

        QualificationType qualificationType =
                requireQualificationType(enrollment);

        /*
         * Resolve the graduation fee configuration applicable
         * to this qualification type and intake.
         */
        List<GraduationFeeItem> feeRules =
                graduationFeeService.effectiveRules(
                        qualificationType,
                        enrollment.getIntake()
                );

        if (feeRules.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No graduation fees are configured for qualification "
                    + qualificationType
                    + " and intake "
                    + enrollment.getIntake().getName()
            );
        }

        BigDecimal total =
                feeRules.stream()
                        .map(GraduationFeeItem::getAmount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (total.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The configured graduation fee total must be greater than zero"
            );
        }

        GraduationApplication application =
                new GraduationApplication();

        application.setEnrollment(enrollment);

        /*
         * The application has only been submitted.
         * It has not yet received academic approval.
         */
        application.setStatus(
                GraduationApplicationStatus.APPLIED
        );

        application.setTotalAmount(total);

        /*
         * Immutable award snapshot.
         */
        application.setAwardTitle(
                awardTitle
        );

        application.setQualificationType(
                qualificationType
        );

        /*
         * Academic result remains pending until approval.
         */
        application.setFinalCumulativeAverage(null);
        application.setAwardClassification(null);
        application.setAcademicApprovalBy(null);
        application.setAcademicApprovedAt(null);

        /*
         * Readiness snapshot.
         */
        application.setRequiredUnits(
                readiness.requiredUnits()
        );

        application.setPassedUnits(
                readiness.passedUnits()
        );

        application.setFailedUnits(
                readiness.failedUnits()
        );

        application.setMissingResults(
                readiness.missingResults()
        );

        application.setRequiredCredits(
                readiness.requiredCredits()
        );

        application.setEarnedCredits(
                readiness.earnedCredits()
        );

        application.setClearanceComplete(
                readiness.clearanceComplete()
        );

        application.setEligibilityAssessedAt(
                readiness.assessedAt()
        );

        /*
         * Copy the effective fee configuration into the
         * application.
         *
         * Future graduation-fee changes must not alter an
         * already submitted application.
         */
        for (GraduationFeeItem feeRule : feeRules) {
            GraduationApplicationFeeItem feeItem =
                    new GraduationApplicationFeeItem();

            feeItem.setFeeItemUuid(
                    feeRule.getFeeItem().getUuid()
            );

            feeItem.setFeeItemCode(
                    feeRule.getFeeItem().getCode()
            );

            feeItem.setFeeItemName(
                    feeRule.getFeeItem().getName()
            );

            feeItem.setAmount(
                    feeRule.getAmount()
            );

            feeItem.setDisplayOrder(
                    feeRule.getDisplayOrder()
            );

            application.addFeeItem(
                    feeItem
            );
        }

        /*
         * Save the application and its fee-item snapshots.
         */
        application =
                applicationRepository.save(
                        application
                );

        /*
         * Create one graduation ledger charge.
         *
         * The enclosing transaction ensures that a failure
         * here also rolls back the application.
         */
        FeeLedger ledgerEntry =
                feeLedgerService.billGraduation(
                        enrollment,
                        total
                );

        application.setLedgerEntry(
                ledgerEntry
        );

        application =
                applicationRepository.save(
                        application
                );

        return response(application);
    }


    @Transactional(readOnly = true)
    public GraduationApplicationResponse getMyApplication(User user) {
        Enrollment enrollment = enrollment(user);
        return applicationRepository.findFirstByEnrollmentIdOrderByGraduationListAcademicYearStartDateDesc(enrollment.getId())
                .map(this::response)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Graduation application not found"));
    }

    private Enrollment enrollment(User user) {
        return enrollmentRepository.findByStudentUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student enrollment not found"));
    }

    private GraduationReadinessResponse requireEligible(Enrollment enrollment) {
        var readiness = readinessService.assess(enrollment);
        if (!readiness.eligible()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                readiness.blockers().isEmpty() ? "Graduation requirements are incomplete" : readiness.blockers().getFirst().message());
        return readiness;
    }

    @Transactional(readOnly = true)
    public Page<ProvisionalGraduationCandidateResponse> applicationsV1(String listType, String search, Long courseId,
                                                                      Long departmentId, String status, Pageable pageable) {
        boolean finalOnly = "FINAL".equalsIgnoreCase(listType);
        Specification<GraduationApplication> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String value = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("enrollment").get("student").get("fullName")), value),
                    cb.like(cb.lower(root.get("enrollment").get("student").get("admissionNumber")), value),
                    cb.like(cb.lower(root.get("enrollment").get("student").get("email")), value)));
        }
        if (courseId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("enrollment").get("course").get("id"), courseId));
        if (departmentId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("enrollment").get("course").get("department").get("id"), departmentId));
        if (status != null && !status.isBlank()) {
            try {
                var parsed = com.owuor.educue.graduation.enums.GraduationApplicationStatus.valueOf(status);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsed));
            } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid graduation application status"); }
        }
        if (finalOnly) spec = spec.and((root, query, cb) -> {
            var balance = query.subquery(BigDecimal.class);
            var ledger = balance.from(FeeLedger.class);
            balance.select(cb.coalesce(cb.sum(cb.diff(ledger.get("debit"), ledger.get("credit"))), BigDecimal.ZERO));
            balance.where(cb.equal(ledger.get("student").get("id"), root.get("enrollment").get("student").get("id")));
            return cb.lessThanOrEqualTo(balance, BigDecimal.ZERO);
        });
        return applicationRepository.findAll(spec, pageable).map(this::candidate);
    }

    @Transactional(readOnly = true)
    public Page<GraduationApplicationOverviewResponse> applicationOverviews(
            String search,
            Long courseId,
            Long departmentId,
            String status,
            Pageable pageable
    ) {
        Specification<GraduationApplication> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {
            String value =
                    "%"
                    + search.trim()
                            .toLowerCase(Locale.ROOT)
                    + "%";

            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.or(
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("enrollment")
                                                            .get("student")
                                                            .get("fullName")
                                            ),
                                            value
                                    ),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("enrollment")
                                                            .get("student")
                                                            .get("admissionNumber")
                                            ),
                                            value
                                    ),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("enrollment")
                                                            .get("course")
                                                            .get("code")
                                            ),
                                            value
                                    ),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("enrollment")
                                                            .get("course")
                                                            .get("name")
                                            ),
                                            value
                                    )
                            )
            );
        }

        if (courseId != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("enrollment")
                                            .get("course")
                                            .get("id"),
                                    courseId
                            )
            );
        }

        if (departmentId != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("enrollment")
                                            .get("course")
                                            .get("department")
                                            .get("id"),
                                    departmentId
                            )
            );
        }

        if (status != null && !status.isBlank()) {
            GraduationApplicationStatus parsedStatus;

            try {
                parsedStatus =
                        GraduationApplicationStatus.valueOf(
                                status.trim()
                                        .toUpperCase(Locale.ROOT)
                        );
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid graduation application status: "
                        + status
                );
            }

            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(
                                    root.get("status"),
                                    parsedStatus
                            )
            );
        }

        return applicationRepository
                .findAll(specification, pageable)
                .map(this::overviewResponse);
    }

    @Transactional(readOnly = true)
    public Page<GraduationApplicationListItemResponse> applications(
            String search,
            Long courseId,
            Long departmentId,
            String status,
            String qualificationType,
            Boolean clearanceComplete,
            Pageable pageable
    ) {
        Specification<GraduationApplication> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        /*
         * Search by student, admission number, email,
         * course or snapshotted award title.
         */
        if (search != null && !search.isBlank()) {
            String value =
                    "%"
                    + search.trim()
                            .toLowerCase(Locale.ROOT)
                    + "%";

            specification =
                    specification.and(
                            (root, query, criteriaBuilder) ->
                                    criteriaBuilder.or(
                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            root.get("enrollment")
                                                                    .get("student")
                                                                    .get("fullName")
                                                    ),
                                                    value
                                            ),

                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            root.get("enrollment")
                                                                    .get("student")
                                                                    .get("admissionNumber")
                                                    ),
                                                    value
                                            ),

                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            root.get("enrollment")
                                                                    .get("student")
                                                                    .get("email")
                                                    ),
                                                    value
                                            ),

                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            root.get("enrollment")
                                                                    .get("course")
                                                                    .get("code")
                                                    ),
                                                    value
                                            ),

                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            root.get("enrollment")
                                                                    .get("course")
                                                                    .get("name")
                                                    ),
                                                    value
                                            ),

                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            root.get("awardTitle")
                                                    ),
                                                    value
                                            )
                                    )
                    );
        }

        if (courseId != null) {
            specification =
                    specification.and(
                            (root, query, criteriaBuilder) ->
                                    criteriaBuilder.equal(
                                            root.get("enrollment")
                                                    .get("course")
                                                    .get("id"),
                                            courseId
                                    )
                    );
        }

        if (departmentId != null) {
            specification =
                    specification.and(
                            (root, query, criteriaBuilder) ->
                                    criteriaBuilder.equal(
                                            root.get("enrollment")
                                                    .get("course")
                                                    .get("department")
                                                    .get("id"),
                                            departmentId
                                    )
                    );
        }

        if (status != null && !status.isBlank()) {
            GraduationApplicationStatus parsedStatus;

            try {
                parsedStatus =
                        GraduationApplicationStatus.valueOf(
                                status.trim()
                                        .toUpperCase(Locale.ROOT)
                        );
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid graduation application status: "
                        + status
                );
            }

            specification =
                    specification.and(
                            (root, query, criteriaBuilder) ->
                                    criteriaBuilder.equal(
                                            root.get("status"),
                                            parsedStatus
                                    )
                    );
        }

        if (qualificationType != null &&
            !qualificationType.isBlank()) {

            QualificationType parsedQualificationType;

            try {
                parsedQualificationType =
                        QualificationType.valueOf(
                                qualificationType.trim()
                                        .toUpperCase(Locale.ROOT)
                        );
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid qualification type: "
                        + qualificationType
                );
            }

            /*
             * Filter using the application snapshot, not the
             * current value on Course.
             */
            specification =
                    specification.and(
                            (root, query, criteriaBuilder) ->
                                    criteriaBuilder.equal(
                                            root.get("qualificationType"),
                                            parsedQualificationType
                                    )
                    );
        }

        if (clearanceComplete != null) {
            specification =
                    specification.and(
                            (root, query, criteriaBuilder) ->
                                    criteriaBuilder.equal(
                                            root.get("clearanceComplete"),
                                            clearanceComplete
                                    )
                    );
        }

        return applicationRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(this::applicationListItem);
    }
    private GraduationApplicationListItemResponse applicationListItem(
            GraduationApplication application
    ) {
        Enrollment enrollment =
                application.getEnrollment();

        BigDecimal studentOutstandingBalance =
                feeLedgerRepository.getOutstandingBalance(
                        enrollment.getStudent().getId()
                );

        if (studentOutstandingBalance == null) {
            studentOutstandingBalance =
                    BigDecimal.ZERO;
        }

        String classification =
                application.getAwardClassification() == null
                        ? null
                        : application
                        .getAwardClassification()
                        .getDisplayName();

        String approvedBy =
                application.getAcademicApprovalBy() == null
                        ? null
                        : application
                        .getAcademicApprovalBy()
                        .getFullName();

        return new GraduationApplicationListItemResponse(

                application.getId(),

                enrollment.getId(),

                enrollment
                        .getStudent()
                        .getAdmissionNumber(),

                enrollment
                        .getStudent()
                        .getFullName(),

                enrollment
                        .getStudent()
                        .getEmail(),

                enrollment
                        .getCourse()
                        .getCode(),

                enrollment
                        .getCourse()
                        .getName(),

                enrollment
                        .getCourse()
                        .getDepartment()
                        .getName(),

                /*
                 * Use the application snapshots.
                 */
                application.getAwardTitle(),

                application.getQualificationType() == null
                        ? null
                        : application
                        .getQualificationType()
                        .name(),

                application
                        .getStatus()
                        .name(),

                application.getRequiredUnits(),
                application.getPassedUnits(),
                application.getFailedUnits(),
                application.getMissingResults(),

                application.getRequiredCredits(),
                application.getEarnedCredits(),

                application.getClearanceComplete(),
                application.getEligibilityAssessedAt(),

                application.getTotalAmount(),
                studentOutstandingBalance,

                application.getLedgerEntry() == null
                        ? null
                        : application
                        .getLedgerEntry()
                        .getDocumentNumber(),

                application.getFinalCumulativeAverage(),
                classification,

                approvedBy,
                application.getAcademicApprovedAt(),

                application.getAppliedAt()
        );
    }


    private ProvisionalGraduationCandidateResponse candidate(GraduationApplication a) {
        var e = a.getEnrollment();
        BigDecimal balance = feeLedgerRepository.getOutstandingBalance(e.getStudent().getId());
        return new ProvisionalGraduationCandidateResponse(a.getId(), e.getStudent().getAdmissionNumber(), e.getStudent().getFullName(),
                e.getCourse().getCode(), e.getCourse().getName(), e.getCourse().getDepartment().getName(), a.getStatus().name(),
                a.getRequiredUnits(), a.getPassedUnits(), a.getFailedUnits(), a.getMissingResults(), a.getRequiredCredits(),
                a.getEarnedCredits(), a.getClearanceComplete(), a.getTotalAmount(), balance,
                a.getLedgerEntry() == null ? null : a.getLedgerEntry().getDocumentNumber(), a.getAppliedAt());
    }

    private GraduationApplicationResponse response(GraduationApplication application) {
        var enrollment = application.getEnrollment();
        return new GraduationApplicationResponse(application.getId(), enrollment.getId(), enrollment.getCourse().getCode(),
                enrollment.getCourse().getQualificationType().name(), application.getStatus().name(), application.getTotalAmount(),
                application.getLedgerEntry() == null ? null : application.getLedgerEntry().getDocumentNumber(), application.getAppliedAt(),
                application.getFeeItems().stream().map(item -> new GraduationApplicationResponse.Item(item.getFeeItemUuid(),
                        item.getFeeItemCode(), item.getFeeItemName(), item.getAmount(), item.getDisplayOrder())).toList());
    }

    private String requireAwardTitle(
            Enrollment enrollment
    ) {
        String awardTitle =
                enrollment
                        .getCourse()
                        .getAwardTitle();

        if (awardTitle == null ||
            awardTitle.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The official award title has not been configured for course "
                    + enrollment.getCourse().getCode()
            );
        }

        return awardTitle.trim();
    }

    private QualificationType requireQualificationType(
            Enrollment enrollment
    ) {
        QualificationType qualificationType =
                enrollment
                        .getCourse()
                        .getQualificationType();

        if (qualificationType == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The qualification type has not been configured for course "
                    + enrollment.getCourse().getCode()
            );
        }

        return qualificationType;
    }

    private GraduationApplicationOverviewResponse overviewResponse(
            GraduationApplication application
    ) {
        Enrollment enrollment =
                application.getEnrollment();

        return new GraduationApplicationOverviewResponse(
                application.getId(),

                enrollment.getStudent()
                        .getAdmissionNumber(),

                enrollment.getStudent()
                        .getFullName(),

                enrollment.getCourse()
                        .getCode(),

                enrollment.getCourse()
                        .getName(),

                application.getStatus()
                        .name(),

                application.getAppliedAt()
        );
    }
}
