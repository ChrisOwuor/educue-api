package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationOrigin;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.academics.repository.LecturerUnitAssignmentRepository;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.finance.service.PeriodFeeItemService;
import com.owuor.educue.institution.enums.AcademicActivityType;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.institution.service.AcademicActivityDeadlineService;
import com.owuor.educue.students.dto.*;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationSpecification;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import static com.owuor.educue.students.dto.HodUnitRegistrationDtos.*;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentUnitRegistrationService {

        private final EnrollmentRepository enrollmentRepository;
        private final CourseUnitPlacementRepository placementRepository;
        private final StudentUnitRegistrationRepository registrationRepository;

        private final FeeLedgerService feeLedgerService;
        private final PeriodFeeItemService periodFeeItemService;

        private final LecturerUnitAssignmentRepository lecturerAssignmentRepository;
        private final AcademicYearRepository academicYearRepository;
        private final AcademicActivityDeadlineService deadlineService;

        /**
         * Registers units that are effective for the student's:
         *
         * current course academic period + intake sequence.
         */
        public RegistrationResponse registerUnits(
                        Long userId,
                        List<Long> placementIds) {
                if (placementIds == null || placementIds.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Select at least one unit.");
                }

                Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Active enrollment not found."));

//                deadlineService.requireOpen(
//                                AcademicActivityType.UNIT_REGISTRATION,
//                                enrollment.getCurrentAcademicYear());

                /*
                 * Check the student's fee-payment eligibility using the
                 * effective PeriodFeeItems for their period and intake.
                 */
                BigDecimal currentPeriodFee = periodFeeItemService
                                .getEffectiveTotalForEnrollment(
                                                enrollment);


                feeLedgerService
                                .validateUnitRegistrationEligibility(
                                                enrollment.getStudent().getId(),
                                                currentPeriodFee);

                Long coursePeriodId = enrollment
                                .getCurrentCourseAcademicPeriod()
                                .getId();

                Long intakeSequence = enrollment
                                .getIntake()
                                .getSequenceNumber();

                /*
                 * Load only units that are actually available for this
                 * student's course period and intake.
                 *
                 * The repository query already validates:
                 * - placement.active
                 * - unit.active
                 * - effectiveFrom <= intake sequence
                 * - effectiveTo is null or intake sequence < effectiveTo
                 */
                List<CourseUnitPlacement> availablePlacements = placementRepository
                                .findEffectiveForPeriodAndIntakeSequence(
                                                coursePeriodId,
                                                intakeSequence);

                Map<Long, CourseUnitPlacement> availableById = availablePlacements.stream()
                                .collect(Collectors.toMap(
                                                CourseUnitPlacement::getId,
                                                placement -> placement));

                /*
                 * Remove duplicate IDs while preserving the submitted order.
                 */
                List<Long> distinctPlacementIds = new ArrayList<>(
                                new LinkedHashSet<>(placementIds));

                List<Long> invalidPlacementIds = distinctPlacementIds.stream()
                                .filter(id -> !availableById.containsKey(id))
                                .toList();

                if (!invalidPlacementIds.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "One or more selected units are not available "
                                                        + "for your current academic period and intake.");
                }

                List<StudentUnitRegistration> registrations = new ArrayList<>();

                for (Long placementId : distinctPlacementIds) {
                        CourseUnitPlacement placement = availableById.get(placementId);

                        boolean alreadyRegistered = registrationRepository
                                        .existsByEnrollmentIdAndCourseUnitPlacementIdAndStatus(
                                                        enrollment.getId(),
                                                        placement.getId(),
                                                        RegistrationStatus.ACTIVE);

                        if (alreadyRegistered) {
                                continue;
                        }

                        StudentUnitRegistration registration = new StudentUnitRegistration();

                        registration.setEnrollment(enrollment);
                        registration.setCourseUnitPlacement(placement);
                        registration.setAttemptType(AttemptType.NORMAL);
                        registration.setStatus(RegistrationStatus.ACTIVE);
                        registration.setRegistrationOrigin(RegistrationOrigin.CURRENT);

                        registrations.add(registration);
                }

                if (registrations.isEmpty()) {
                        return new RegistrationResponse(
                                        "The selected units are already registered.",
                                        0);
                }

                registrationRepository.saveAll(registrations);

                return new RegistrationResponse(
                                registrations.size()
                                                + " unit(s) registered successfully.",
                                registrations.size());
        }

        @Transactional
        public RegistrationResponse registerLegacyUnits(
                Long userId,
                List<Long> placementIds
        ) {
                if (placementIds == null || placementIds.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Select at least one unit."
                        );
                }

                Enrollment enrollment = enrollmentRepository
                        .findByStudentUserId(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Active enrollment not found."
                                )
                        );

                var currentCoursePeriod =
                        enrollment.getCurrentCourseAcademicPeriod();

                Long currentCoursePeriodId =
                        currentCoursePeriod.getId();

                Long courseId = currentCoursePeriod
                        .getCourse()
                        .getId();

                Integer currentPosition =
                        currentCoursePeriod.getPosition();

                Long intakeSequence = enrollment
                        .getIntake()
                        .getSequenceNumber();

                /*
                 * Load every placement the student is allowed to register:
                 * current period plus all previous periods in the same course.
                 */
                List<CourseUnitPlacement> availablePlacements =
                        placementRepository.findEffectiveUpToCurrentPeriod(
                                courseId,
                                currentPosition,
                                intakeSequence
                        );

                Map<Long, CourseUnitPlacement> availableById =
                        availablePlacements.stream()
                                .collect(Collectors.toMap(
                                        CourseUnitPlacement::getId,
                                        placement -> placement
                                ));

                /*
                 * Remove duplicate submitted IDs while preserving order.
                 */
                List<Long> distinctPlacementIds =
                        new ArrayList<>(
                                new LinkedHashSet<>(placementIds)
                        );

                List<Long> invalidPlacementIds =
                        distinctPlacementIds.stream()
                                .filter(id -> !availableById.containsKey(id))
                                .toList();

                if (!invalidPlacementIds.isEmpty()) {
                        throw new IllegalArgumentException(
                                "One or more selected units are not available " +
                                "for your course, academic progress, or intake."
                        );
                }

                List<StudentUnitRegistration> registrations =
                        new ArrayList<>();

                int skippedAlreadyRegistered = 0;

                for (Long placementId : distinctPlacementIds) {
                        CourseUnitPlacement placement =
                                availableById.get(placementId);

                        boolean alreadyRegistered =
                                registrationRepository
                                        .existsByEnrollmentIdAndCourseUnitPlacementIdAndStatus(
                                                enrollment.getId(),
                                                placement.getId(),
                                                RegistrationStatus.ACTIVE
                                        );

                        if (alreadyRegistered) {
                                skippedAlreadyRegistered++;
                                continue;
                        }

                        boolean belongsToCurrentPeriod =
                                Objects.equals(
                                        placement
                                                .getCourseAcademicPeriod()
                                                .getId(),
                                        currentCoursePeriodId
                                );

                        StudentUnitRegistration registration =
                                new StudentUnitRegistration();

                        registration.setEnrollment(enrollment);
                        registration.setCourseUnitPlacement(placement);

                        /*
                         * A previous-period unit is not automatically a RETAKE.
                         *
                         * RETAKE should normally mean the student attempted and failed
                         * the unit before. A unit from an older period being registered
                         * for the first time can still be NORMAL.
                         */
                        registration.setAttemptType(
                                AttemptType.NORMAL
                        );

                        registration.setStatus(
                                RegistrationStatus.ACTIVE
                        );

                        registration.setRegistrationOrigin(
                                belongsToCurrentPeriod
                                        ? RegistrationOrigin.CURRENT
                                        : RegistrationOrigin.LEGACY
                        );

                        registrations.add(registration);
                }

                if (registrations.isEmpty()) {
                        return new RegistrationResponse(
                                "The selected units are already registered.",
                                0
                        );
                }

                registrationRepository.saveAll(registrations);

                String message =
                        registrations.size() +
                        " unit(s) registered successfully.";

                if (skippedAlreadyRegistered > 0) {
                        message += " " +
                                   skippedAlreadyRegistered +
                                   " already registered unit(s) were skipped.";
                }

                return new RegistrationResponse(
                        message,
                        registrations.size()
                );
        }


        /**
         * Returns units the student has already registered.
         *
         * These remain attached to the exact placement version that
         * applied when the student registered.
         */
        @Transactional(readOnly = true)
        public List<StudentUnitResponse> getRegisteredUnits(
                        Long userId) {
                return registrationRepository
                                .findByEnrollmentStudentUserIdAndStatusOrderByCourseUnitPlacementUnitCode(
                                                userId,
                                                RegistrationStatus.ACTIVE)
                                .stream()
                                .map(this::toStudentUnit)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<StudentUnitResponse> getLegacyRegistrations(
                        Long userId) {
                return registrationRepository
                                .findByEnrollmentStudentUserIdAndStatusAndRegistrationOriginOrderByCourseUnitPlacementUnitCode(
                                                userId,
                                                RegistrationStatus.ACTIVE,
                                                RegistrationOrigin.LEGACY)
                                .stream()
                                .map(this::toStudentUnit)
                                .toList();
        }

        @Transactional(readOnly = true)
        public RegisteredStudentsResponse getRegisteredStudents(
                        Long placementId,
                        User requester) {
                assertCanViewPlacement(
                                placementId,
                                requester);

                CourseUnitPlacement placement = placementRepository.findById(placementId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Course unit placement not found."));

                List<StudentUnitRegistration> registrations = registrationRepository
                                .findByCourseUnitPlacementIdAndStatusOrderByEnrollmentStudentAdmissionNumberAsc(
                                                placementId,
                                                RegistrationStatus.ACTIVE);

                return RegisteredStudentsResponse.builder()
                                .courseUnitPlacementId(
                                                placement.getId())
                                .unitCode(
                                                placement.getUnit().getCode())
                                .unitName(
                                                placement.getUnit().getName())
                                .courseAcademicPeriodId(
                                                placement
                                                                .getCourseAcademicPeriod()
                                                                .getId())
                                .academicPeriodName(
                                                placement
                                                                .getCourseAcademicPeriod()
                                                                .getAcademicPeriod()
                                                                .getName())
                                .totalRegistered(
                                                registrations.size())
                                .students(
                                                registrations.stream()
                                                                .map(this::toRegisteredStudent)
                                                                .toList())
                                .build();
        }

        @Transactional(readOnly = true)
        public void assertCanViewPlacement(
                        Long placementId,
                        User requester) {
                String role = requester.getRole().getName();

                if ("ADMIN".equals(role)
                                || "HOD".equals(role)) {
                        return;
                }

                if (!"TRAINER".equals(role)) {
                        throw new AccessDeniedException(
                                        "You cannot view this unit roster.");
                }

                var currentYear = academicYearRepository
                                .findByCurrentTrue()
                                .orElseThrow(() -> new IllegalStateException(
                                                "No current academic year is configured."));

                boolean assigned = lecturerAssignmentRepository
                                .findByLecturerIdOrderByAssignedAtDesc(
                                                requester.getId())
                                .stream()
                                .anyMatch(assignment -> assignment
                                                .getCourseUnitPlacement()
                                                .getId()
                                                .equals(placementId)
                                                && assignment.isActiveFor(
                                                                currentYear));

                if (!assigned) {
                        throw new AccessDeniedException(
                                        "This unit is not allocated to you "
                                                        + "in the current academic year.");
                }
        }

        @Transactional(readOnly = true)
        public List<IntakeOption> hodIntakes(User requester) {
                Long departmentId = hodDepartment(requester);
                var spec = (org.springframework.data.jpa.domain.Specification<Enrollment>) (root, query, cb) -> cb.and(
                                cb.equal(root.get("course").get("department").get("id"), departmentId),
                                cb.equal(root.get("status"), com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE));
                return enrollmentRepository.findAll(spec, Pageable.unpaged()).getContent().stream()
                                .collect(Collectors.groupingBy(Enrollment::getIntake, LinkedHashMap::new, Collectors.counting()))
                                .entrySet().stream().map(entry -> new IntakeOption(entry.getKey().getUuid(), entry.getKey().getName(),
                                                entry.getKey().getAcademicYear().getCode(), entry.getValue()))
                                .sorted(Comparator.comparing(IntakeOption::academicYearCode).reversed()).toList();
        }

        @Transactional(readOnly = true)
        public List<EnrollmentOption> hodEnrollments(User requester, UUID intakeUuid, String search) {
                Long departmentId = hodDepartment(requester);
                String value = search == null ? "" : search.trim().toLowerCase();
                var spec = (org.springframework.data.jpa.domain.Specification<Enrollment>) (root, query, cb) -> {
                        var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                        predicates.add(cb.equal(root.get("course").get("department").get("id"), departmentId));
                        predicates.add(cb.equal(root.get("intake").get("uuid"), intakeUuid));
                        predicates.add(cb.equal(root.get("status"), com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE));
                        if (!value.isBlank()) predicates.add(cb.or(
                                        cb.like(cb.lower(root.get("student").get("fullName")), "%" + value + "%"),
                                        cb.like(cb.lower(root.get("student").get("admissionNumber")), "%" + value + "%")));
                        return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
                return enrollmentRepository.findAll(spec, Pageable.unpaged()).getContent().stream()
                                .sorted(Comparator.comparing(e -> e.getStudent().getAdmissionNumber()))
                                .map(e -> new EnrollmentOption(e.getUuid(), e.getStudent().getAdmissionNumber(), e.getStudent().getFullName(),
                                                e.getCourse().getCode(), e.getCurrentCourseAcademicPeriod().getAcademicPeriod().getCode(),
                                                e.getIntake().getUuid(), e.getIntake().getName())).toList();
        }

        @Transactional(readOnly = true)
        public List<UnitOption> hodAvailableUnits(User requester, SelectionRequest request) {
                List<Enrollment> selected = selectedEnrollments(requester, request.enrollmentUuids());
                Map<UUID, CourseUnitPlacement> intersection = null;
                for (Enrollment enrollment : selected) {
                        Map<UUID, CourseUnitPlacement> available = placementRepository.findEffectiveForPeriodAndIntakeSequence(
                                                enrollment.getCurrentCourseAcademicPeriod().getId(), enrollment.getIntake().getSequenceNumber())
                                        .stream().collect(Collectors.toMap(CourseUnitPlacement::getUuid, placement -> placement));
                        if (intersection == null) intersection = new LinkedHashMap<>(available);
                        else intersection.keySet().retainAll(available.keySet());
                }
                if (intersection == null) return List.of();
                return intersection.values().stream().sorted(Comparator.comparing(p -> p.getUnit().getCode()))
                                .map(p -> new UnitOption(p.getUuid(), p.getUnit().getCode(), p.getUnit().getName(),
                                                p.getUnit().getCreditHours(), p.getCourseAcademicPeriod().getAcademicPeriod().getCode())).toList();
        }

        public RegisterResponse hodRegister(User requester, RegisterRequest request) {
                List<Enrollment> selected = selectedEnrollments(requester, request.enrollmentUuids());
                if (request.placementUuids() == null || request.placementUuids().isEmpty()) throw new IllegalArgumentException("Select at least one unit");
                Set<UUID> requested = new LinkedHashSet<>(request.placementUuids());
                int created = 0, skipped = 0;
                for (Enrollment enrollment : selected) {
                        Map<UUID, CourseUnitPlacement> available = placementRepository.findEffectiveForPeriodAndIntakeSequence(
                                                enrollment.getCurrentCourseAcademicPeriod().getId(), enrollment.getIntake().getSequenceNumber())
                                        .stream().collect(Collectors.toMap(CourseUnitPlacement::getUuid, placement -> placement));
                        if (!available.keySet().containsAll(requested)) throw new IllegalArgumentException("Selected units are not available to every selected student for their intake and current period");
                        for (UUID placementUuid : requested) {
                                CourseUnitPlacement placement = available.get(placementUuid);
                                if (registrationRepository.existsByEnrollmentIdAndCourseUnitPlacementIdAndStatus(enrollment.getId(), placement.getId(), RegistrationStatus.ACTIVE)) { skipped++; continue; }
                                var registration = new StudentUnitRegistration();
                                registration.setEnrollment(enrollment); registration.setCourseUnitPlacement(placement);
                                registration.setAttemptType(AttemptType.NORMAL); registration.setStatus(RegistrationStatus.ACTIVE);
                                registration.setRegistrationOrigin(RegistrationOrigin.CURRENT);
                                registrationRepository.save(registration); created++;
                        }
                }
                return new RegisterResponse(selected.size(), created, skipped, created + " registration(s) created without fee debits; " + skipped + " duplicate(s) skipped");
        }

        private List<Enrollment> selectedEnrollments(User requester, List<UUID> uuids) {
                if (uuids == null || uuids.isEmpty()) throw new IllegalArgumentException("Select at least one student");
                Long departmentId = hodDepartment(requester);
                List<Enrollment> selected = new ArrayList<>();
                UUID intake = null;
                for (UUID uuid : new LinkedHashSet<>(uuids)) {
                        Enrollment enrollment = enrollmentRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException("Student enrollment not found"));
                        if (!enrollment.getCourse().getDepartment().getId().equals(departmentId)) throw new AccessDeniedException("Student belongs to another department");
                        if (enrollment.getStatus() != com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE) throw new IllegalArgumentException("Only active students can be registered");
                        if (intake == null) intake = enrollment.getIntake().getUuid();
                        else if (!intake.equals(enrollment.getIntake().getUuid())) throw new IllegalArgumentException("All selected students must belong to the same intake");
                        selected.add(enrollment);
                }
                return selected;
        }

        private Long hodDepartment(User requester) {
                if (requester == null || requester.getDepartment() == null) throw new AccessDeniedException("HOD account is not assigned to a department");
                return requester.getDepartment().getId();
        }

        public List<StudentUnitRegistrationResponse> getAllRegistrations() {

                return registrationRepository
                                .findByStatusOrderByRegisteredAtDesc(
                                                RegistrationStatus.ACTIVE)
                                .stream()
                                .map(this::toRegistration)
                                .toList();
        }

        @Transactional(readOnly = true)
        public Page<StudentUnitRegistrationResponse> getRegistrations(
                        StudentUnitRegistrationFilterRequest filter,
                        Pageable pageable) {
                return registrationRepository
                                .findAll(
                                                StudentUnitRegistrationSpecification.withFilters(
                                                                filter.getSearch(),
                                                                filter.getCourseId(),
                                                                filter.getCourseAcademicPeriodId(),
                                                                filter.getCourseUnitPlacementId(),
                                                                filter.getStudentId(),
                                                                filter.getAcademicYearId(),
                                                                filter.getIntakeId(),
                                                                filter.getAttemptType(),
                                                                filter.getStatus(),
                                                                filter.getRegistrationOrigin()),
                                                pageable)
                                .map(this::toRegistration);
        }
        private StudentUnitResponse toResponse(
                CourseUnitPlacement placement,
                StudentUnitRegistration registration
        ) {
                return StudentUnitResponse.builder()
                        .registrationId(
                                registration == null
                                        ? null
                                        : registration.getId()
                        )
                        .courseUnitPlacementId(
                                placement.getId()
                        )
                        .courseUnitPlacementUuid(
                                placement.getUuid()
                        )
                        .courseAcademicPeriodUuid(
                                placement
                                        .getCourseAcademicPeriod()
                                        .getUuid()
                        )
                        .unitId(
                                placement.getUnit().getId()
                        )
                        .unitCode(
                                placement.getUnit().getCode()
                        )
                        .unitName(
                                placement.getUnit().getName()
                        )
                        .creditHours(
                                placement.getUnit().getCreditHours()
                        )
                        .unitType(
                                placement.getUnitType()
                        )
                        .academicPeriodCode(
                                placement
                                        .getCourseAcademicPeriod()
                                        .getAcademicPeriod()
                                        .getCode()
                        )
                        .academicPeriodName(
                                placement
                                        .getCourseAcademicPeriod()
                                        .getAcademicPeriod()
                                        .getName()
                        )
                        .attemptType(
                                registration == null
                                        ? null
                                        : String.valueOf(registration.getAttemptType())
                        )
                        .registrationOrigin(
                                registration == null
                                        ? null
                                        : String.valueOf(registration.getRegistrationOrigin())
                        )
                        .build();
        }

        private StudentUnitResponse toStudentUnit(
                        StudentUnitRegistration registration) {
                CourseUnitPlacement placement = registration.getCourseUnitPlacement();

                var period = placement
                                .getCourseAcademicPeriod()
                                .getAcademicPeriod();

                return StudentUnitResponse.builder()
                                .registrationId(registration.getId())
                                .courseUnitPlacementId(
                                                placement.getId())
                                .courseUnitPlacementUuid(
                                                placement.getUuid())
                                .courseAcademicPeriodUuid(
                                                placement
                                                                .getCourseAcademicPeriod()
                                                                .getUuid())
                                .unitId(
                                                placement.getUnit().getId())
                                .unitCode(
                                                placement.getUnit().getCode())
                                .unitName(
                                                placement.getUnit().getName())
                                .creditHours(
                                                placement
                                                                .getUnit()
                                                                .getCreditHours())
                                .unitType(
                                                placement.getUnitType())
                                .academicPeriodCode(
                                                period.getCode())
                                .academicPeriodName(
                                                period.getName())
                                .attemptType(registration.getAttemptType().name())
                                .registrationOrigin(registration.getRegistrationOrigin().name())
                                .build();
        }

        private RegisteredStudentResponse toRegisteredStudent(
                        StudentUnitRegistration registration) {
                return RegisteredStudentResponse.builder()
                                .registrationId(
                                                registration.getId())
                                .studentId(
                                                registration
                                                                .getEnrollment()
                                                                .getStudent()
                                                                .getId())
                                .admissionNumber(
                                                registration
                                                                .getEnrollment()
                                                                .getStudent()
                                                                .getAdmissionNumber())
                                .studentName(
                                                registration
                                                                .getEnrollment()
                                                                .getStudent()
                                                                .getFullName())
                                .attemptType(
                                                registration
                                                                .getAttemptType()
                                                                .name())
                                .status(
                                                registration
                                                                .getStatus()
                                                                .name())
                                .registrationOrigin(
                                                registration
                                                                .getRegistrationOrigin()
                                                                .name())
                                .registeredAt(
                                                registration.getRegisteredAt())
                                .build();
        }

        private StudentUnitRegistrationResponse toRegistration(
                        StudentUnitRegistration registration) {
                Enrollment enrollment = registration.getEnrollment();

                CourseUnitPlacement placement = registration.getCourseUnitPlacement();

                return StudentUnitRegistrationResponse.builder()
                                .registrationId(
                                                registration.getId())
                                .studentId(
                                                enrollment.getStudent().getId())
                                .courseUnitPlacementId(
                                                placement.getId())
                                .studentName(
                                                enrollment
                                                                .getStudent()
                                                                .getFullName())
                                .admissionNumber(
                                                enrollment
                                                                .getStudent()
                                                                .getAdmissionNumber())
                                .course(
                                                enrollment
                                                                .getCourse()
                                                                .getName())
                                .currentAcademicPeriod(
                                                enrollment
                                                                .getCurrentCourseAcademicPeriod()
                                                                .getAcademicPeriod()
                                                                .getName())
                                .academicPeriod(
                                                placement
                                                                .getCourseAcademicPeriod()
                                                                .getAcademicPeriod()
                                                                .getName())
                                .unitCode(
                                                placement.getUnit().getCode())
                                .unitName(
                                                placement.getUnit().getName())
                                .attemptType(
                                                registration
                                                                .getAttemptType()
                                                                .name())
                                .status(
                                                registration
                                                                .getStatus()
                                                                .name())
                                .registeredAt(
                                                registration.getRegisteredAt())
                                .build();
        }


}
