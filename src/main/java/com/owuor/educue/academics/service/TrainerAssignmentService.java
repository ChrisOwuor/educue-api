package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.BulkTrainerAssignmentRequest;
import com.owuor.educue.academics.dto.BulkTrainerAssignmentResponse;
import com.owuor.educue.academics.dto.CourseUnitAllocationResponse;
import com.owuor.educue.academics.dto.CreateTrainerAssignmentRequest;
import com.owuor.educue.academics.dto.TrainerAssignmentResponse;
import com.owuor.educue.academics.dto.TrainerUnitGroupResponse;
import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.entity.LecturerUnitAssignment;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.academics.repository.LecturerUnitAssignmentRepository;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainerAssignmentService {

    private final LecturerUnitAssignmentRepository assignmentRepository;
    private final CourseUnitPlacementRepository placementRepository;

    private final AcademicYearRepository academicYearRepository;
    private final IntakeRepository intakeRepository;
    private final UserRepository userRepository;

    public TrainerAssignmentResponse create(
            CreateTrainerAssignmentRequest request,
            User assignedBy
    ) {
        User lecturer = requireLecturer(
                request.getLecturerId()
        );

        CourseUnitPlacement placement =
                placementRepository
                        .findDetailedByUuid(
                                request.getCourseUnitPlacementUuid()
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Course unit placement not found."
                                )
                        );

        validatePlacementAvailable(placement);

        AcademicYear fromYear = findYear(
                request.getEffectiveFromAcademicYearUuid()
        );

        AcademicYear toYear =
                request.getEffectiveToAcademicYearUuid() == null
                        ? null
                        : findYear(
                        request.getEffectiveToAcademicYearUuid()
                );

        validateAcademicYearRange(
                fromYear,
                toYear
        );

        List<LecturerUnitAssignment> overlapping =
                assignmentRepository
                        .findByCourseUnitPlacementIdAndEnabledTrueOrderByAssignedAtDesc(
                                placement.getId()
                        )
                        .stream()
                        .filter(existing ->
                                rangesOverlap(
                                        existing,
                                        fromYear,
                                        toYear
                                )
                        )
                        .toList();

        boolean sameLecturerAlreadyAssigned =
                overlapping.stream()
                        .anyMatch(existing ->
                                existing.getLecturer()
                                        .getId()
                                        .equals(lecturer.getId())
                        );

        if (sameLecturerAlreadyAssigned) {
            throw new IllegalArgumentException(
                    "This lecturer is already allocated to this unit "
                    + "for that academic-year range."
            );
        }

        /*
         * Replacing a lecturer disables the overlapping allocation
         * while preserving it as assignment history.
         */
        overlapping.forEach(existing ->
                existing.setEnabled(false)
        );

        if (!overlapping.isEmpty()) {
            assignmentRepository.saveAll(overlapping);
        }

        LecturerUnitAssignment assignment =
                createAssignment(
                        placement,
                        lecturer,
                        fromYear,
                        toYear,
                        assignedBy
                );

        assignment = assignmentRepository.save(
                assignment
        );

        return toResponse(
                assignment,
                currentYearOr(fromYear)
        );
    }

    public BulkTrainerAssignmentResponse createBulk(
            BulkTrainerAssignmentRequest request,
            User assignedBy
    ) {
        User lecturer = requireLecturer(
                request.lecturerId()
        );

        AcademicYear fromYear = findYear(
                request.effectiveFromAcademicYearUuid()
        );

        AcademicYear toYear =
                request.effectiveToAcademicYearUuid() == null
                        ? null
                        : findYear(
                        request.effectiveToAcademicYearUuid()
                );

        validateAcademicYearRange(
                fromYear,
                toYear
        );

        int created = 0;
        List<UUID> skipped = new ArrayList<>();

        LinkedHashSet<UUID> placementUuids =
                new LinkedHashSet<>(
                        request.courseUnitPlacementUuids()
                );

        for (UUID placementUuid : placementUuids) {
            CourseUnitPlacement placement =
                    placementRepository
                            .findDetailedByUuid(placementUuid)
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "Course unit placement not found: "
                                            + placementUuid
                                    )
                            );

            validatePlacementAvailable(placement);

            boolean occupied =
                    assignmentRepository
                            .findByCourseUnitPlacementIdAndEnabledTrueOrderByAssignedAtDesc(
                                    placement.getId()
                            )
                            .stream()
                            .anyMatch(existing ->
                                    rangesOverlap(
                                            existing,
                                            fromYear,
                                            toYear
                                    )
                            );

            /*
             * Bulk allocation does not replace existing lecturers.
             * Occupied placements are reported as skipped.
             */
            if (occupied) {
                skipped.add(placementUuid);
                continue;
            }

            LecturerUnitAssignment assignment =
                    createAssignment(
                            placement,
                            lecturer,
                            fromYear,
                            toYear,
                            assignedBy
                    );

            assignmentRepository.save(assignment);
            created++;
        }

        return new BulkTrainerAssignmentResponse(
                created,
                skipped.size(),
                skipped
        );
    }

    /**
     * CourseUnitPlacement is the root dataset.
     *
     * Every active placement is returned even when it has no
     * current lecturer assignment.
     */
    @Transactional(readOnly = true)
    public List<CourseUnitAllocationResponse> getAllocationView(
            String search
    ) {
        AcademicYear currentYear =
                academicYearRepository
                        .findByCurrentTrue()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No current academic year is configured."
                                )
                        );

        String term = search == null
                ? ""
                : search.trim()
                .toLowerCase(Locale.ROOT);

        return placementRepository
                .findAllForAllocationView()
                .stream()
                .filter(placement ->
                        term.isEmpty()
                        || allocationMatches(
                                placement,
                                term,
                                currentYear
                        )
                )
                .map(placement ->
                        toAllocationResponse(
                                placement,
                                currentYear
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrainerAssignmentResponse> getMine(
            Long lecturerId,
            Long intakeId
    ) {
        AcademicYear currentYear =
                academicYearRepository
                        .findByCurrentTrue()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No current academic year is configured."
                                )
                        );

        Long intakeSequence =
                intakeId == null
                        ? null
                        : intakeRepository
                        .findById(intakeId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Intake not found."
                                )
                        )
                        .getSequenceNumber();

        return assignmentRepository
                .findByLecturerIdOrderByAssignedAtDesc(
                        lecturerId
                )
                .stream()
                .filter(assignment ->
                        assignment.isActiveFor(currentYear)
                )
                .filter(assignment ->
                        intakeSequence == null
                        || placementAppliesToIntake(
                                assignment.getCourseUnitPlacement(),
                                intakeSequence
                        )
                )
                .map(assignment ->
                        toResponse(
                                assignment,
                                currentYear
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrainerUnitGroupResponse> getMyGroupedUnits(Long lecturerId) {
        AcademicYear currentYear = academicYearRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException("No current academic year is configured."));

        var grouped = assignmentRepository.findByLecturerIdOrderByAssignedAtDesc(lecturerId).stream()
                .filter(assignment -> assignment.isActiveFor(currentYear))
                .collect(java.util.stream.Collectors.groupingBy(
                        assignment -> assignment.getCourseUnitPlacement().getUnit().getId(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        return grouped.values().stream().map(assignments -> {
            var unit = assignments.get(0).getCourseUnitPlacement().getUnit();
            var placements = assignments.stream()
                    .map(LecturerUnitAssignment::getCourseUnitPlacement)
                    .collect(java.util.stream.Collectors.toMap(
                            CourseUnitPlacement::getId,
                            placement -> placement,
                            (first, ignored) -> first,
                            LinkedHashMap::new
                    )).values().stream().map(placement -> {
                        var coursePeriod = placement.getCourseAcademicPeriod();
                        var course = coursePeriod.getCourse();
                        var period = coursePeriod.getAcademicPeriod();
                        return new TrainerUnitGroupResponse.Placement(
                                placement.getId(), placement.getUuid(), course.getId(), course.getUuid(),
                                course.getCode(), course.getName(), period.getCode(), period.getName());
                    }).toList();
            return new TrainerUnitGroupResponse(unit.getId(), unit.getUuid(), unit.getCode(), unit.getName(),
                    currentYear.getCode(), placements);
        }).toList();
    }

    private LecturerUnitAssignment createAssignment(
            CourseUnitPlacement placement,
            User lecturer,
            AcademicYear fromYear,
            AcademicYear toYear,
            User assignedBy
    ) {
        LecturerUnitAssignment assignment =
                new LecturerUnitAssignment();

        assignment.setCourseUnitPlacement(placement);
        assignment.setLecturer(lecturer);

        assignment.setEffectiveFromAcademicYear(fromYear);
        assignment.setEffectiveToAcademicYear(toYear);

        assignment.setStartYear(
                fromYear.getStartYear()
        );

        assignment.setAssignedBy(assignedBy);
        assignment.setEnabled(true);

        return assignment;
    }

    private Optional<LecturerUnitAssignment> activeAssignment(
            CourseUnitPlacement placement,
            AcademicYear currentYear
    ) {
        return placement.getLecturerAssignments()
                .stream()
                .filter(assignment ->
                        assignment.isActiveFor(currentYear)
                )
                .max(
                        Comparator.comparing(
                                LecturerUnitAssignment::getAssignedAt
                        )
                );
    }

    private CourseUnitAllocationResponse toAllocationResponse(
            CourseUnitPlacement placement,
            AcademicYear currentYear
    ) {
        var coursePeriod =
                placement.getCourseAcademicPeriod();

        var course =
                coursePeriod.getCourse();

        var academicPeriod =
                coursePeriod.getAcademicPeriod();

        var placementFrom =
                placement.getEffectiveFromIntake();

        var placementTo =
                placement.getEffectiveToIntake();

        var assignment =
                activeAssignment(
                        placement,
                        currentYear
                ).orElse(null);

        var lecturer =
                assignment == null
                        ? null
                        : assignment.getLecturer();

        var assignedBy =
                assignment == null
                        ? null
                        : assignment.getAssignedBy();

        var assignmentFrom =
                assignment == null
                        ? null
                        : assignment.getEffectiveFromAcademicYear();

        var assignmentTo =
                assignment == null
                        ? null
                        : assignment.getEffectiveToAcademicYear();

        return CourseUnitAllocationResponse.builder()
                .courseUnitPlacementId(
                        placement.getId()
                )
                .courseUnitPlacementUuid(
                        placement.getUuid()
                )

                .unitId(
                        placement.getUnit().getId()
                )
                .unitUuid(
                        placement.getUnit().getUuid()
                )
                .unitCode(
                        placement.getUnit().getCode()
                )
                .unitName(
                        placement.getUnit().getName()
                )
                .unitType(
                        placement.getUnitType()
                )

                .courseAcademicPeriodId(
                        coursePeriod.getId()
                )
                .courseAcademicPeriodUuid(
                        coursePeriod.getUuid()
                )
                .academicPeriodCode(
                        academicPeriod.getCode()
                )
                .academicPeriodName(
                        academicPeriod.getName()
                )

                .courseId(
                        course.getId()
                )
                .courseUuid(
                        course.getUuid()
                )
                .courseCode(
                        course.getCode()
                )
                .courseName(
                        course.getName()
                )

                .placementEffectiveFromIntakeId(
                        placementFrom.getId()
                )
                .placementEffectiveFromIntakeName(
                        placementFrom.getName()
                )
                .placementEffectiveToIntakeId(
                        placementTo == null
                                ? null
                                : placementTo.getId()
                )
                .placementEffectiveToIntakeName(
                        placementTo == null
                                ? null
                                : placementTo.getName()
                )

                .assignmentId(
                        assignment == null
                                ? null
                                : assignment.getId()
                )
                .lecturerId(
                        lecturer == null
                                ? null
                                : lecturer.getId()
                )
                .lecturerName(
                        lecturer == null
                                ? null
                                : lecturer.getFullName()
                )

                .effectiveFromAcademicYearUuid(
                        assignmentFrom == null
                                ? null
                                : assignmentFrom.getUuid()
                )
                .effectiveFromAcademicYearCode(
                        assignmentFrom == null
                                ? null
                                : assignmentFrom.getCode()
                )
                .effectiveToAcademicYearUuid(
                        assignmentTo == null
                                ? null
                                : assignmentTo.getUuid()
                )
                .effectiveToAcademicYearCode(
                        assignmentTo == null
                                ? null
                                : assignmentTo.getCode()
                )

                .assignedAt(
                        assignment == null
                                ? null
                                : assignment.getAssignedAt()
                )
                .assignedById(
                        assignedBy == null
                                ? null
                                : assignedBy.getId()
                )
                .assignedByName(
                        assignedBy == null
                                ? null
                                : assignedBy.getFullName()
                )
                .build();
    }

    private TrainerAssignmentResponse toResponse(
            LecturerUnitAssignment assignment,
            AcademicYear currentYear
    ) {
        CourseUnitPlacement placement =
                assignment.getCourseUnitPlacement();

        var coursePeriod =
                placement.getCourseAcademicPeriod();

        var course =
                coursePeriod.getCourse();

        var period =
                coursePeriod.getAcademicPeriod();

        var placementFrom =
                placement.getEffectiveFromIntake();

        var placementTo =
                placement.getEffectiveToIntake();

        var assignedBy =
                assignment.getAssignedBy();

        return TrainerAssignmentResponse.builder()
                .id(assignment.getId())

                .trainerId(
                        assignment.getLecturer().getId()
                )
                .trainerName(
                        assignment.getLecturer().getFullName()
                )

                .courseUnitPlacementId(
                        placement.getId()
                )
                .courseUnitPlacementUuid(
                        placement.getUuid()
                )

                .unitId(
                        placement.getUnit().getId()
                )
                .unitUuid(
                        placement.getUnit().getUuid()
                )
                .unitCode(
                        placement.getUnit().getCode()
                )
                .unitName(
                        placement.getUnit().getName()
                )
                .unitType(
                        placement.getUnitType()
                )

                .courseAcademicPeriodId(
                        coursePeriod.getId()
                )
                .courseAcademicPeriodUuid(
                        coursePeriod.getUuid()
                )
                .academicPeriodCode(
                        period.getCode()
                )
                .academicPeriodName(
                        period.getName()
                )

                .courseId(course.getId())
                .courseUuid(course.getUuid())
                .courseCode(course.getCode())
                .courseName(course.getName())

                .placementEffectiveFromIntakeId(
                        placementFrom.getId()
                )
                .placementEffectiveFromIntakeName(
                        placementFrom.getName()
                )
                .placementEffectiveToIntakeId(
                        placementTo == null
                                ? null
                                : placementTo.getId()
                )
                .placementEffectiveToIntakeName(
                        placementTo == null
                                ? null
                                : placementTo.getName()
                )

                .effectiveFromAcademicYearUuid(
                        assignment.getEffectiveFromAcademicYear()
                                .getUuid()
                )
                .effectiveFrom(
                        assignment.getEffectiveFromAcademicYear()
                                .getCode()
                )
                .effectiveToAcademicYearUuid(
                        assignment.getEffectiveToAcademicYear() == null
                                ? null
                                : assignment.getEffectiveToAcademicYear()
                                .getUuid()
                )
                .effectiveTo(
                        assignment.getEffectiveToAcademicYear() == null
                                ? null
                                : assignment.getEffectiveToAcademicYear()
                                .getCode()
                )

                .active(
                        assignment.isActiveFor(currentYear)
                )
                .assignedAt(
                        assignment.getAssignedAt()
                )

                .assignedById(
                        assignedBy == null
                                ? null
                                : assignedBy.getId()
                )
                .assignedByName(
                        assignedBy == null
                                ? null
                                : assignedBy.getFullName()
                )
                .build();
    }

    private boolean allocationMatches(
            CourseUnitPlacement placement,
            String term,
            AcademicYear currentYear
    ) {
        var course =
                placement.getCourseAcademicPeriod()
                        .getCourse();

        var period =
                placement.getCourseAcademicPeriod()
                        .getAcademicPeriod();

        return contains(course.getName(), term)
               || contains(course.getCode(), term)
               || contains(period.getName(), term)
               || contains(period.getCode(), term)
               || contains(placement.getUnit().getName(), term)
               || contains(placement.getUnit().getCode(), term)
               || activeAssignment(
                placement,
                currentYear
        )
                       .map(assignment ->
                               contains(
                                       assignment.getLecturer()
                                               .getFullName(),
                                       term
                               )
                       )
                       .orElse(false);
    }

    private boolean placementAppliesToIntake(
            CourseUnitPlacement placement,
            long intakeSequence
    ) {
        if (!placement.isActive()
            || !placement.getUnit().isActive()) {
            return false;
        }

        long fromSequence =
                placement.getEffectiveFromIntake()
                        .getSequenceNumber();

        Long toSequence =
                placement.getEffectiveToIntake() == null
                        ? null
                        : placement.getEffectiveToIntake()
                        .getSequenceNumber();

        return fromSequence <= intakeSequence
               && (
                       toSequence == null
                       || intakeSequence < toSequence
               );
    }

    private boolean rangesOverlap(
            LecturerUnitAssignment existing,
            AcademicYear from,
            AcademicYear to
    ) {
        int newStart = from.getStartYear();

        int newEnd = to == null
                ? Integer.MAX_VALUE
                : to.getStartYear();

        int existingStart =
                existing.getEffectiveFromAcademicYear()
                        .getStartYear();

        int existingEnd =
                existing.getEffectiveToAcademicYear() == null
                        ? Integer.MAX_VALUE
                        : existing.getEffectiveToAcademicYear()
                        .getStartYear();

        return existingStart <= newEnd
               && newStart <= existingEnd;
    }

    private boolean contains(
            String value,
            String term
    ) {
        return value != null
               && value.toLowerCase(Locale.ROOT)
                       .contains(term);
    }

    private User requireLecturer(Long lecturerId) {
        User lecturer =
                userRepository.findById(lecturerId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Lecturer not found."
                                )
                        );

        if (lecturer.getRole() == null
            || !"TRAINER".equals(
                lecturer.getRole().getName()
        )) {
            throw new IllegalArgumentException(
                    "The selected user does not have the TRAINER role."
            );
        }

        if (!lecturer.isActive()) {
            throw new IllegalArgumentException(
                    "The selected lecturer account is inactive."
            );
        }

        return lecturer;
    }

    private void validatePlacementAvailable(
            CourseUnitPlacement placement
    ) {
        if (!placement.isActive()) {
            throw new IllegalArgumentException(
                    "The selected course unit placement is inactive."
            );
        }

        if (!placement.getUnit().isActive()) {
            throw new IllegalArgumentException(
                    "The selected unit is inactive."
            );
        }

        if (!placement.getCourseAcademicPeriod()
                .getAcademicPeriod()
                .isActive()) {
            throw new IllegalArgumentException(
                    "The selected academic period is inactive."
            );
        }
    }

    private void validateAcademicYearRange(
            AcademicYear fromYear,
            AcademicYear toYear
    ) {
        if (toYear != null
            && toYear.getStartYear()
               < fromYear.getStartYear()) {
            throw new IllegalArgumentException(
                    "Effective-to academic year cannot precede "
                    + "effective-from academic year."
            );
        }
    }

    private AcademicYear findYear(UUID uuid) {
        return academicYearRepository
                .findByUuid(uuid)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Academic year not found."
                        )
                );
    }

    private AcademicYear currentYearOr(
            AcademicYear fallback
    ) {
        return academicYearRepository
                .findByCurrentTrue()
                .orElse(fallback);
    }
}
