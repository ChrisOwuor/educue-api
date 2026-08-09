package com.owuor.educue.finance.service;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.enums.IntakeStatus;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.admissions.service.IntakeService;
import com.owuor.educue.finance.dto.PeriodFeeStructureResponse;
import com.owuor.educue.finance.dto.SavePeriodFeeStructureRequest;
import com.owuor.educue.finance.dto.StudentFeesResponse;
import com.owuor.educue.finance.entity.FeeItem;
import com.owuor.educue.finance.entity.PeriodFeeItem;
import com.owuor.educue.finance.repository.FeeItemRepository;
import com.owuor.educue.finance.repository.PeriodFeeItemRepository;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodFeeStructureService {

    private static final int DISPLAY_ORDER_STEP = 10;

    private final PeriodFeeItemRepository periodFeeItemRepository;
    private final FeeItemRepository feeItemRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final IntakeRepository intakeRepository;
    private final IntakeService intakeService;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public PeriodFeeStructureResponse getStructure(
            UUID courseAcademicPeriodUuid,
            Long intakeId
    ) {
        CourseAcademicPeriod courseAcademicPeriod =
                courseAcademicPeriodRepository
                        .findDetailedByUuid(courseAcademicPeriodUuid)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Course academic period not found"
                        ));

        Intake intake = getIntake(intakeId);

        List<PeriodFeeItem> effectiveRules =
                periodFeeItemRepository.findEffectiveFees(
                        courseAcademicPeriodUuid,
                        intake.getSequenceNumber()
                );

        validateNoOverlappingEffectiveRules(
                effectiveRules
        );

        return mapResponse(
                courseAcademicPeriod,
                intake,
                effectiveRules,
                PeriodFeeStructureResponse.ChangeSummary.empty()
        );
    }

    @Transactional
    public PeriodFeeStructureResponse saveStructure(
            SavePeriodFeeStructureRequest request
    ) {
        /*
         * Locking the parent course period prevents concurrent saves for
         * the same structure even when no fee rows currently exist.
         */
        CourseAcademicPeriod courseAcademicPeriod =
                courseAcademicPeriodRepository
                        .findDetailedByUuidForUpdate(
                                request.courseAcademicPeriodUuid()
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Course academic period not found"
                        ));

        Intake selectedIntake =
                getIntake(request.intakeId());

        boolean firstCoursePeriod = isFirstCoursePeriod(
                courseAcademicPeriod.getCourse().getId(),
                courseAcademicPeriod.getUuid()
        );
        if (firstCoursePeriod && selectedIntake.getStatus() != IntakeStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "First-period fees can only be configured while the intake is draft"
            );
        }

        /*
         * Later, this is where invoice-lock validation should be added:
         *
         * assertStructureEditable(
         *     courseAcademicPeriod,
         *     selectedIntake
         * );
         */

        LinkedHashMap<UUID, RequestedLine> desiredLines =
                prepareDesiredLines(request.items());

        Map<UUID, FeeItem> requestedFeeItems =
                loadRequestedFeeItems(
                        desiredLines.keySet()
                );

        long selectedSequence =
                selectedIntake.getSequenceNumber();

        List<PeriodFeeItem> effectiveRules =
                periodFeeItemRepository.findEffectiveFees(
                        courseAcademicPeriod.getUuid(),
                        selectedSequence
                );

        validateNoOverlappingEffectiveRules(
                effectiveRules
        );

        Map<UUID, PeriodFeeItem> existingByFeeItem =
                effectiveRules.stream()
                        .collect(Collectors.toMap(
                                rule -> rule.getFeeItem().getUuid(),
                                Function.identity()
                        ));

        List<PeriodFeeItem> rowsToInsert =
                new ArrayList<>();

        int added = 0;
        int amountChanged = 0;
        int removed = 0;
        int unchanged = 0;

        /*
         * Process rules currently applicable to the selected intake.
         */
        for (PeriodFeeItem existingRule : effectiveRules) {
            UUID feeItemUuid =
                    existingRule.getFeeItem().getUuid();

            RequestedLine desired =
                    desiredLines.remove(feeItemUuid);

            /*
             * The fee currently applies, but was omitted from the
             * submitted desired structure.
             */
            if (desired == null) {
                removeFromSelectedIntake(
                        existingRule,
                        selectedIntake,
                        selectedSequence
                );

                removed++;
                continue;
            }

            BigDecimal existingAmount =
                    normalizeAmount(
                            existingRule.getAmount()
                    );

            /*
             * The fee remains applicable with the same amount.
             */
            if (existingAmount.compareTo(desired.amount()) == 0) {
                existingRule.setDisplayOrder(
                        desired.displayOrder()
                );

                unchanged++;
                continue;
            }

            FeeItem feeItem =
                    requestedFeeItems.get(feeItemUuid);

            validateFeeItemCanBeChanged(feeItem);

            long existingStartSequence =
                    existingRule
                            .getEffectiveFromIntake()
                            .getSequenceNumber();

            if (existingStartSequence == selectedSequence) {
                /*
                 * This rule already begins at the selected intake.
                 *
                 * This is a correction to that intake's own configuration,
                 * so update the row directly instead of creating an empty
                 * [selected intake, selected intake) range.
                 */
                existingRule.setAmount(
                        desired.amount()
                );

                existingRule.setDisplayOrder(
                        desired.displayOrder()
                );
            } else {
                /*
                 * This amount was inherited from an earlier intake.
                 *
                 * Close the inherited version at the selected intake and
                 * create a replacement from the selected intake onward.
                 */
                Intake previousEnd =
                        existingRule.getEffectiveToIntake();

                existingRule.setEffectiveToIntake(
                        selectedIntake
                );

                PeriodFeeItem replacement =
                        createRule(
                                courseAcademicPeriod,
                                existingRule.getFeeItem(),
                                desired.amount(),
                                selectedIntake,
                                previousEnd,
                                existingRule.isMandatory(),
                                desired.displayOrder()
                        );

                rowsToInsert.add(replacement);
            }

            amountChanged++;
        }

        /*
         * Any desired lines still remaining did not previously apply to
         * the selected intake. They are new additions.
         */
        for (RequestedLine desired : desiredLines.values()) {
            FeeItem feeItem =
                    requestedFeeItems.get(
                            desired.feeItemUuid()
                    );

            validateFeeItemCanBeAdded(feeItem);

            Intake nextBoundary =
                    periodFeeItemRepository
                            .findFirstByCourseAcademicPeriod_IdAndFeeItem_IdAndEffectiveFromIntake_SequenceNumberGreaterThanOrderByEffectiveFromIntake_SequenceNumberAsc(
                                    courseAcademicPeriod.getId(),
                                    feeItem.getId(),
                                    selectedSequence
                            )
                            .map(
                                    PeriodFeeItem::getEffectiveFromIntake
                            )
                            .orElse(null);

            PeriodFeeItem newRule =
                    createRule(
                            courseAcademicPeriod,
                            feeItem,
                            desired.amount(),
                            selectedIntake,
                            nextBoundary,
                            true,
                            desired.displayOrder()
                    );

            rowsToInsert.add(newRule);
            added++;
        }

        try {
            /*
             * First flush closed, updated and deleted rows.
             *
             * This avoids temporary overlap before inserting replacement
             * rows once the PostgreSQL exclusion constraint is enabled.
             */
            periodFeeItemRepository.flush();

            if (!rowsToInsert.isEmpty()) {
                periodFeeItemRepository.saveAll(
                        rowsToInsert
                );

                periodFeeItemRepository.flush();
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The submitted fee structure conflicts with an existing intake range",
                    exception
            );
        }

        List<PeriodFeeItem> resultingRules =
                periodFeeItemRepository.findEffectiveFees(
                        courseAcademicPeriod.getUuid(),
                        selectedSequence
                );

        return mapResponse(
                courseAcademicPeriod,
                selectedIntake,
                resultingRules,
                new PeriodFeeStructureResponse.ChangeSummary(
                        added,
                        amountChanged,
                        removed,
                        unchanged
                )
        );
    }


    /** Saves the fee structure and records finance confirmation atomically. */
    @Transactional
    public PeriodFeeStructureResponse saveAndConfirmStructure(
            SavePeriodFeeStructureRequest request
    ) {
        PeriodFeeStructureResponse response = saveStructure(request);
        // The intake-level confirmation is the readiness flag used to publish
        // the intake, so it applies only to its first course period. Later
        // periods remain independently editable after the intake is open.
        if (isFirstCoursePeriod(response.courseId(), request.courseAcademicPeriodUuid())) {
            intakeService.confirmFees(request.intakeId(), response.courseId(), true);
        }
        return response;
    }

    private boolean isFirstCoursePeriod(Long courseId, UUID periodUuid) {
        return courseAcademicPeriodRepository
                .findFirstByCourseIdOrderByPositionAsc(courseId)
                .map(firstPeriod -> firstPeriod.getUuid().equals(periodUuid))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The course progression structure is not configured"
                ));
    }

    @Transactional(readOnly = true)
    public StudentFeesResponse getFees(
            User user
    ) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found for user ID: " + user.getId()));
        CourseAcademicPeriod currentPeriod =
                enrollment.getCurrentCourseAcademicPeriod();

        Intake intake =
                enrollment.getIntake();

        List<PeriodFeeItem> feeRules =
                periodFeeItemRepository.findEffectiveFees(
                        currentPeriod.getUuid(),
                        intake.getSequenceNumber()
                );

        List<StudentFeesResponse.Item> items =
                feeRules.stream()
                        .map(rule ->
                                new StudentFeesResponse.Item(
                                        rule.getId(),
                                        rule.getFeeItem().getUuid(),
                                        rule.getFeeItem().getCode(),
                                        rule.getFeeItem().getName(),
                                        rule.getAmount()
                                )
                        )
                        .toList();

        BigDecimal total =
                items.stream()
                        .map(StudentFeesResponse.Item::amount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new StudentFeesResponse(
                currentPeriod.getUuid(),
                currentPeriod
                        .getAcademicPeriod()
                        .getCode(),
                currentPeriod
                        .getAcademicPeriod()
                        .getName(),
                intake.getName(),
                total,
                items
        );
    }


    private LinkedHashMap<UUID, RequestedLine> prepareDesiredLines(
            List<SavePeriodFeeStructureRequest.Item> submittedItems
    ) {
        LinkedHashMap<UUID, RequestedLine> result =
                new LinkedHashMap<>();

        for (int index = 0;
             index < submittedItems.size();
             index++) {

            SavePeriodFeeStructureRequest.Item submitted =
                    submittedItems.get(index);

            UUID feeItemUuid =
                    submitted.feeItemUuid();

            if (result.containsKey(feeItemUuid)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A fee item can only appear once in a fee structure"
                );
            }

            int displayOrder =
                    (index + 1) * DISPLAY_ORDER_STEP;

            result.put(
                    feeItemUuid,
                    new RequestedLine(
                            feeItemUuid,
                            normalizeAmount(
                                    submitted.amount()
                            ),
                            displayOrder
                    )
            );
        }

        return result;
    }

    private Map<UUID, FeeItem> loadRequestedFeeItems(
            Collection<UUID> requestedUuids
    ) {
        if (requestedUuids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, FeeItem> found =
                feeItemRepository
                        .findAllByUuidIn(requestedUuids)
                        .stream()
                        .collect(Collectors.toMap(
                                FeeItem::getUuid,
                                Function.identity()
                        ));

        List<UUID> missing =
                requestedUuids.stream()
                        .filter(uuid -> !found.containsKey(uuid))
                        .toList();

        if (!missing.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Fee item(s) not found: " + missing
            );
        }

        return found;
    }

    private void removeFromSelectedIntake(
            PeriodFeeItem existingRule,
            Intake selectedIntake,
            long selectedSequence
    ) {
        long existingStartSequence =
                existingRule
                        .getEffectiveFromIntake()
                        .getSequenceNumber();

        if (existingStartSequence == selectedSequence) {
            /*
             * A row cannot start and end at the same intake.
             *
             * Since this rule was created specifically for the selected
             * intake, removing it means deleting that version.
             */
            periodFeeItemRepository.delete(
                    existingRule
            );
        } else {
            /*
             * The fee was inherited from an earlier intake.
             *
             * End it immediately before the selected intake.
             */
            existingRule.setEffectiveToIntake(
                    selectedIntake
            );
        }
    }

    private PeriodFeeItem createRule(
            CourseAcademicPeriod courseAcademicPeriod,
            FeeItem feeItem,
            BigDecimal amount,
            Intake effectiveFrom,
            Intake effectiveTo,
            boolean mandatory,
            int displayOrder
    ) {
        PeriodFeeItem rule =
                new PeriodFeeItem();

        rule.setCourseAcademicPeriod(
                courseAcademicPeriod
        );

        rule.setFeeItem(feeItem);
        rule.setAmount(amount);

        rule.setEffectiveFromIntake(
                effectiveFrom
        );

        rule.setEffectiveToIntake(
                effectiveTo
        );

        /*
         * These fields remain internal for now.
         * The new frontend does not expose them.
         */
        rule.setMandatory(mandatory);
        rule.setDisplayOrder(displayOrder);

        return rule;
    }

    private void validateFeeItemCanBeAdded(
            FeeItem feeItem
    ) {
        if (!feeItem.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inactive fee item cannot be added: "
                    + feeItem.getName()
            );
        }
    }

    private void validateFeeItemCanBeChanged(
            FeeItem feeItem
    ) {
        if (!feeItem.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inactive fee item amount cannot be changed: "
                    + feeItem.getName()
            );
        }
    }

    private void validateNoOverlappingEffectiveRules(
            List<PeriodFeeItem> effectiveRules
    ) {
        Set<UUID> seen = new HashSet<>();

        List<String> duplicates =
                effectiveRules.stream()
                        .filter(rule -> !seen.add(
                                rule.getFeeItem().getUuid()
                        ))
                        .map(rule -> rule.getFeeItem().getName())
                        .distinct()
                        .toList();

        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Overlapping fee rules already exist for: "
                    + String.join(", ", duplicates)
            );
        }
    }

    private PeriodFeeStructureResponse mapResponse(
            CourseAcademicPeriod courseAcademicPeriod,
            Intake intake,
            List<PeriodFeeItem> rules,
            PeriodFeeStructureResponse.ChangeSummary changes
    ) {
        List<PeriodFeeStructureResponse.Item> items =
                rules.stream()
                        .map(rule -> {
                            Intake sourceIntake =
                                    rule.getEffectiveFromIntake();

                            boolean inherited =
                                    !sourceIntake
                                            .getSequenceNumber()
                                            .equals(
                                                    intake.getSequenceNumber()
                                            );

                            return new PeriodFeeStructureResponse.Item(
                                    rule.getId(),

                                    rule.getFeeItem().getUuid(),
                                    rule.getFeeItem().getCode(),
                                    rule.getFeeItem().getName(),
                                    rule.getFeeItem().getCategory(),

                                    normalizeAmount(
                                            rule.getAmount()
                                    ),

                                    inherited,
                                    sourceIntake.getId(),
                                    sourceIntake.getName()
                            );
                        })
                        .toList();

        BigDecimal total =
                items.stream()
                        .map(
                                PeriodFeeStructureResponse.Item::amount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        var course =
                courseAcademicPeriod.getCourse();

        var academicPeriod =
                courseAcademicPeriod.getAcademicPeriod();

        return new PeriodFeeStructureResponse(
                courseAcademicPeriod.getUuid(),

                course.getId(),
                course.getCode(),
                course.getName(),

                academicPeriod.getUuid(),
                academicPeriod.getCode(),
                academicPeriod.getName(),

                intake.getId(),
                intake.getName(),
                intake.getSequenceNumber(),

                total,
                items,
                changes
        );
    }

    private Intake getIntake(Long intakeId) {
        return intakeRepository
                .findById(intakeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Intake not found"
                ));
    }

    private BigDecimal normalizeAmount(
            BigDecimal amount
    ) {
        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private record RequestedLine(
            UUID feeItemUuid,
            BigDecimal amount,
            int displayOrder
    ) {
    }
}
