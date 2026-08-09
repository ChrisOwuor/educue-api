package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.CourseUnitStructureResponse;
import com.owuor.educue.academics.dto.SaveCourseUnitStructureRequest;
import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.entity.Unit;
import com.owuor.educue.academics.enums.UnitType;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.academics.repository.UnitRepository;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.admissions.service.IntakeService;
import com.owuor.educue.common.exception.ApiException;
import com.owuor.educue.institution.enums.AcademicActivityType;
import com.owuor.educue.institution.service.AcademicActivityDeadlineService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseUnitPlacementService {

    private final CourseUnitPlacementRepository placementRepository;
    private final CourseRepository courseRepository;
    private final CourseAcademicPeriodRepository coursePeriodRepository;
    private final UnitRepository unitRepository;

    private final IntakeRepository intakeRepository;
    private final IntakeService intakeService;

    private final AcademicActivityDeadlineService deadlineService;

    public CourseUnitStructureResponse getStructure(
            UUID courseUuid,
            UUID courseAcademicPeriodUuid,
            Long intakeId
    ) {
        Course course =
                findCourse(courseUuid);

        CourseAcademicPeriod coursePeriod =
                coursePeriodRepository
                        .findDetailedByUuid(
                                courseAcademicPeriodUuid
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Course academic period not found"
                                )
                        );

        validateCoursePeriod(
                course,
                coursePeriod
        );

        Intake intake =
                findIntake(intakeId);

        List<CourseUnitPlacement> placements =
                placementRepository
                        .findEffectiveForPeriodAndIntakeSequence(
                                coursePeriod.getId(),
                                intake.getSequenceNumber()
                        );

        validateNoOverlappingEffectivePlacements(
                placements
        );

        return toStructureResponse(
                course,
                coursePeriod,
                intake,
                placements,
                CourseUnitStructureResponse.ChangeSummary.empty()
        );
    }

    @Transactional
    public CourseUnitStructureResponse saveStructure(
            UUID courseUuid,
            SaveCourseUnitStructureRequest request
    ) {
        Course course =
                findCourse(courseUuid);

        if (!course.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Units cannot be configured for an inactive course"
            );
        }

        /*
         * Prevent two administrators from modifying the same
         * period structure concurrently.
         */
        CourseAcademicPeriod coursePeriod =
                coursePeriodRepository
                        .findDetailedByUuidForUpdate(
                                request.courseAcademicPeriodUuid()
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Course academic period not found"
                                )
                        );

        validateCoursePeriod(
                course,
                coursePeriod
        );

        Intake selectedIntake =
                findIntake(request.intakeId());

        /*
         * Nothing may change after the unit-setup deadline.
         */
//        deadlineService.requireOpen(
//                AcademicActivityType.UNIT_ADDITION,
//                selectedIntake.getAcademicYear()
//        );

        LinkedHashMap<UUID, DesiredUnit> desiredUnits =
                prepareDesiredUnits(
                        request.units()
                );

        Map<UUID, Unit> unitsByUuid =
                loadUnits(
                        desiredUnits.keySet()
                );

        long selectedSequence =
                selectedIntake.getSequenceNumber();

        List<CourseUnitPlacement> effectivePlacements =
                placementRepository
                        .findEffectiveForPeriodAndIntakeSequence(
                                coursePeriod.getId(),
                                selectedSequence
                        );

        validateNoOverlappingEffectivePlacements(
                effectivePlacements
        );

        List<CourseUnitPlacement> rowsToDelete =
                new ArrayList<>();

        List<CourseUnitPlacement> rowsToInsert =
                new ArrayList<>();

        int added = 0;
        int typeChanged = 0;
        int removed = 0;
        int unchanged = 0;

        for (CourseUnitPlacement existing :
                effectivePlacements) {

            UUID unitUuid =
                    existing.getUnit().getUuid();

            DesiredUnit desired =
                    desiredUnits.remove(unitUuid);

            long existingStartSequence =
                    existing
                            .getEffectiveFromIntake()
                            .getSequenceNumber();

            boolean startsAtSelectedIntake =
                    existingStartSequence
                    == selectedSequence;

            /*
             * Unit removed from submitted structure.
             */
            if (desired == null) {

                if (startsAtSelectedIntake) {
                    /*
                     * It was created for this intake.
                     * Physically remove the mistaken entry.
                     */
                    rowsToDelete.add(existing);
                } else {
                    /*
                     * It was inherited from an earlier intake.
                     * Preserve history and stop it from this intake.
                     */
                    existing.setEffectiveToIntake(
                            selectedIntake
                    );
                }

                removed++;
                continue;
            }

            /*
             * Nothing changed.
             */
            if (existing.getUnitType()
                == desired.unitType()) {

                unchanged++;
                continue;
            }

            /*
             * Type changes are not direct edits.
             *
             * Remove/close the existing placement then create a
             * fresh placement version.
             */
            Intake previousEnd =
                    existing.getEffectiveToIntake();

            if (startsAtSelectedIntake) {
                rowsToDelete.add(existing);
            } else {
                existing.setEffectiveToIntake(
                        selectedIntake
                );
            }

            rowsToInsert.add(
                    createPlacement(
                            coursePeriod,
                            existing.getUnit(),
                            desired.unitType(),
                            selectedIntake,
                            previousEnd
                    )
            );

            typeChanged++;
        }

        /*
         * Anything left in desiredUnits is newly introduced
         * for the selected intake.
         */
        for (DesiredUnit desired :
                desiredUnits.values()) {

            Unit unit =
                    unitsByUuid.get(
                            desired.unitUuid()
                    );

            if (!unit.isActive()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Inactive unit cannot be added: "
                        + unit.getCode()
                );
            }

            /*
             * If a future placement version already exists,
             * stop the new row at that future starting intake.
             */
            Intake nextBoundary =
                    placementRepository
                            .findFirstByCourseAcademicPeriod_IdAndUnit_IdAndEffectiveFromIntake_SequenceNumberGreaterThanOrderByEffectiveFromIntake_SequenceNumberAsc(
                                    coursePeriod.getId(),
                                    unit.getId(),
                                    selectedSequence
                            )
                            .map(
                                    CourseUnitPlacement::getEffectiveFromIntake
                            )
                            .orElse(null);

            rowsToInsert.add(
                    createPlacement(
                            coursePeriod,
                            unit,
                            desired.unitType(),
                            selectedIntake,
                            nextBoundary
                    )
            );

            added++;
        }

        persistChanges(
                rowsToDelete,
                rowsToInsert
        );

        List<CourseUnitPlacement> result =
                placementRepository
                        .findEffectiveForPeriodAndIntakeSequence(
                                coursePeriod.getId(),
                                selectedSequence
                        );

        return toStructureResponse(
                course,
                coursePeriod,
                selectedIntake,
                result,
                new CourseUnitStructureResponse.ChangeSummary(
                        added,
                        typeChanged,
                        removed,
                        unchanged
                )
        );
    }

    /**
     * Saves the selected intake structure and records academic confirmation
     * in the same transaction. If confirmation fails, placement changes roll
     * back as well.
     */
    @Transactional
    public CourseUnitStructureResponse saveAndConfirmStructure(
            UUID courseUuid,
            SaveCourseUnitStructureRequest request
    ) {
        CourseUnitStructureResponse response = saveStructure(courseUuid, request);
        Course course = findCourse(courseUuid);
        intakeService.confirmAcademic(request.intakeId(), course.getId(), true);
        return response;
    }

    private void persistChanges(
            List<CourseUnitPlacement> rowsToDelete,
            List<CourseUnitPlacement> rowsToInsert
    ) {
        try {
            /*
             * Delete rows first so a replacement can use the
             * same period + unit + starting intake key.
             */
            if (!rowsToDelete.isEmpty()) {
                placementRepository.deleteAll(
                        rowsToDelete
                );

                placementRepository.flush();
            }

            /*
             * Flush closed inherited placements.
             */
            placementRepository.flush();

            if (!rowsToInsert.isEmpty()) {
                placementRepository.saveAll(
                        rowsToInsert
                );

                placementRepository.flush();
            }
        } catch (DataIntegrityViolationException exception) {
            String databaseMessage =
                    exception.getMostSpecificCause()
                            .getMessage();

            if (databaseMessage != null
                && databaseMessage.contains(
                    "uk_course_unit_placement_start"
            )) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "A placement for this unit already starts at the selected intake"
                );
            }

            if (databaseMessage != null
                && databaseMessage
                        .toLowerCase(Locale.ROOT)
                        .contains("foreign key")) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "The placement cannot be physically deleted because it is already referenced by another record"
                );
            }

            throw exception;
        }
    }

    private CourseUnitPlacement createPlacement(
            CourseAcademicPeriod coursePeriod,
            Unit unit,
            UnitType unitType,
            Intake effectiveFrom,
            Intake effectiveTo
    ) {
        CourseUnitPlacement placement =
                new CourseUnitPlacement();

        placement.setCourseAcademicPeriod(
                coursePeriod
        );

        placement.setUnit(unit);
        placement.setUnitType(unitType);

        placement.setEffectiveFromIntake(
                effectiveFrom
        );

        placement.setEffectiveToIntake(
                effectiveTo
        );

        placement.setActive(true);

        return placement;
    }

    private LinkedHashMap<UUID, DesiredUnit>
    prepareDesiredUnits(
            List<SaveCourseUnitStructureRequest.Item> submitted
    ) {
        LinkedHashMap<UUID, DesiredUnit> result =
                new LinkedHashMap<>();

        for (SaveCourseUnitStructureRequest.Item item :
                submitted) {

            if (result.containsKey(
                    item.unitUuid()
            )) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "A unit can only appear once in the structure"
                );
            }

            result.put(
                    item.unitUuid(),
                    new DesiredUnit(
                            item.unitUuid(),
                            item.unitType()
                    )
            );
        }

        return result;
    }

    private Map<UUID, Unit> loadUnits(
            Collection<UUID> uuids
    ) {
        if (uuids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Unit> found =
                unitRepository
                        .findAllByUuidIn(uuids)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Unit::getUuid,
                                        Function.identity()
                                )
                        );

        List<UUID> missing =
                uuids.stream()
                        .filter(uuid ->
                                !found.containsKey(uuid)
                        )
                        .toList();

        if (!missing.isEmpty()) {
            throw new EntityNotFoundException(
                    "Unit(s) not found: " + missing
            );
        }

        return found;
    }

    private void validateNoOverlappingEffectivePlacements(
            List<CourseUnitPlacement> placements
    ) {
        Set<UUID> seenUnits =
                new HashSet<>();

        List<String> duplicates =
                placements.stream()
                        .filter(placement ->
                                !seenUnits.add(
                                        placement
                                                .getUnit()
                                                .getUuid()
                                )
                        )
                        .map(placement ->
                                placement
                                        .getUnit()
                                        .getCode()
                        )
                        .distinct()
                        .toList();

        if (!duplicates.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Overlapping unit placements exist for: "
                    + String.join(", ", duplicates)
            );
        }
    }

    private CourseUnitStructureResponse toStructureResponse(
            Course course,
            CourseAcademicPeriod coursePeriod,
            Intake intake,
            List<CourseUnitPlacement> placements,
            CourseUnitStructureResponse.ChangeSummary changes
    ) {
        List<CourseUnitStructureResponse.Item> items =
                placements.stream()
                        .map(placement ->
                                toItem(
                                        placement,
                                        intake
                                )
                        )
                        .toList();

        return new CourseUnitStructureResponse(
                course.getUuid(),
                course.getCode(),
                course.getName(),

                coursePeriod.getUuid(),
                coursePeriod
                        .getAcademicPeriod()
                        .getCode(),
                coursePeriod
                        .getAcademicPeriod()
                        .getName(),

                intake.getId(),
                intake.getName(),
                intake.getSequenceNumber(),

                items,
                changes
        );
    }

    private CourseUnitStructureResponse.Item toItem(
            CourseUnitPlacement placement,
            Intake selectedIntake
    ) {
        Intake sourceIntake =
                placement.getEffectiveFromIntake();

        boolean inherited =
                !sourceIntake
                        .getSequenceNumber()
                        .equals(
                                selectedIntake
                                        .getSequenceNumber()
                        );

        return new CourseUnitStructureResponse.Item(
                placement.getId(),
                placement.getUuid(),

                placement.getUnit().getId(),
                placement.getUnit().getUuid(),
                placement.getUnit().getCode(),
                placement.getUnit().getName(),
                placement.getUnit().getCreditHours(),

                placement.getUnitType(),

                inherited,

                sourceIntake.getId(),
                sourceIntake.getName()
        );
    }

    private Course findCourse(UUID courseUuid) {
        return courseRepository
                .findByUuid(courseUuid)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Course not found"
                        )
                );
    }

    private Intake findIntake(Long intakeId) {
        return intakeRepository
                .findById(intakeId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Intake not found"
                        )
                );
    }

    private void validateCoursePeriod(
            Course course,
            CourseAcademicPeriod coursePeriod
    ) {
        if (!coursePeriod
                .getCourse()
                .getId()
                .equals(course.getId())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Course academic period does not belong to the selected course"
            );
        }

        if (!coursePeriod
                .getAcademicPeriod()
                .isActive()) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "An inactive academic period cannot receive units"
            );
        }
    }

    private record DesiredUnit(
            UUID unitUuid,
            UnitType unitType
    ) {
    }
}
