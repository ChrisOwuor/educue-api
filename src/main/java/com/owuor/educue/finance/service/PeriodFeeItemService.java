package com.owuor.educue.finance.service;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.repository.IntakeCourseRepository;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.finance.dto.ChangePeriodFeeAmountRequest;
import com.owuor.educue.finance.dto.CreatePeriodFeeItemRequest;
import com.owuor.educue.finance.dto.EffectivePeriodFeesResponse;
import com.owuor.educue.finance.dto.PeriodFeeItemResponse;
import com.owuor.educue.finance.dto.StopPeriodFeeItemRequest;
import com.owuor.educue.finance.dto.UpdatePeriodFeeItemRequest;
import com.owuor.educue.finance.entity.FeeItem;
import com.owuor.educue.finance.entity.PeriodFeeItem;
import com.owuor.educue.finance.repository.FeeItemRepository;
import com.owuor.educue.finance.repository.PeriodFeeItemRepository;
import com.owuor.educue.students.entity.Enrollment;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PeriodFeeItemService {

    private static final long OPEN_ENDED_SEQUENCE = Long.MAX_VALUE;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "amount",
            "displayOrder",
            "createdAt",
            "updatedAt",
            "feeItem.code",
            "feeItem.name",
            "effectiveFromIntake.sequenceNumber"
    );

    private final PeriodFeeItemRepository periodFeeItemRepository;
    private final FeeItemRepository feeItemRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final IntakeRepository intakeRepository;
    private final IntakeCourseRepository intakeCourseRepository;
    private final Clock clock;

    @Transactional
    public PeriodFeeItemResponse create(
            CreatePeriodFeeItemRequest request
    ) {
        CourseAcademicPeriod courseAcademicPeriod =
                getCourseAcademicPeriod(request.courseAcademicPeriodUuid());

        FeeItem feeItem = getFeeItem(request.feeItemUuid());

        if (!feeItem.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inactive fee items cannot be added to a fee structure"
            );
        }

        Intake effectiveFrom =
                getIntake(request.effectiveFromIntakeId());

        Intake effectiveTo = request.effectiveToIntakeId() != null
                ? getIntake(request.effectiveToIntakeId())
                : null;

        validateCourseAvailableForIntake(
                courseAcademicPeriod,
                effectiveFrom
        );

        validateRange(
                courseAcademicPeriod.getId(),
                feeItem.getId(),
                effectiveFrom,
                effectiveTo,
                null
        );

        PeriodFeeItem item = new PeriodFeeItem();
        item.setCourseAcademicPeriod(courseAcademicPeriod);
        item.setFeeItem(feeItem);
        item.setAmount(normalizeAmount(request.amount()));
        item.setEffectiveFromIntake(effectiveFrom);
        item.setEffectiveToIntake(effectiveTo);
        item.setMandatory(request.mandatory());
        item.setDisplayOrder(request.displayOrder());

        return saveAndMap(item);
    }

    @Transactional(readOnly = true)
    public PeriodFeeItemResponse getById(Long id) {
        return PeriodFeeItemResponse.from(getDetailed(id));
    }

    @Transactional(readOnly = true)
    public Page<PeriodFeeItemResponse> search(
            String search,
            UUID courseUuid,
            UUID courseAcademicPeriodUuid,
            UUID feeItemUuid,
            Long effectiveAtIntakeId,
            Boolean mandatory,
            int page,
            int size,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                resolveSort(sort)
        );

        Long intakeSequence = effectiveAtIntakeId != null
                ? getIntake(effectiveAtIntakeId).getSequenceNumber()
                : null;

        Specification<PeriodFeeItem> specification = buildSpecification(
                search,
                courseUuid,
                courseAcademicPeriodUuid,
                feeItemUuid,
                intakeSequence,
                mandatory
        );

        return periodFeeItemRepository
                .findAll(specification, pageable)
                .map(PeriodFeeItemResponse::from);
    }
    /**
     * Direct update is allowed only before the rule's starting intake begins.
     *
     * Existing/current rules must use changeAmount() or stop().
     */
    @Transactional
    public PeriodFeeItemResponse update(
            Long id,
            UpdatePeriodFeeItemRequest request
    ) {
        PeriodFeeItem item = getDetailed(id);

        ensureRuleHasNotStarted(item);

        Intake effectiveTo = request.effectiveToIntakeId() != null
                ? getIntake(request.effectiveToIntakeId())
                : null;

        validateRange(
                item.getCourseAcademicPeriod().getId(),
                item.getFeeItem().getId(),
                item.getEffectiveFromIntake(),
                effectiveTo,
                item.getId()
        );

        item.setAmount(normalizeAmount(request.amount()));
        item.setEffectiveToIntake(effectiveTo);
        item.setMandatory(request.mandatory());
        item.setDisplayOrder(request.displayOrder());

        return saveAndMap(item);
    }

    /**
     * Splits an existing range and creates a new fee version.
     *
     * Example:
     *
     * Existing:
     * 35,000 [Sep 2026, infinity)
     *
     * Change from Sep 2027:
     * 35,000 [Sep 2026, Sep 2027)
     * 40,000 [Sep 2027, infinity)
     */
    @Transactional
    public PeriodFeeItemResponse changeAmount(
            Long id,
            ChangePeriodFeeAmountRequest request
    ) {
        PeriodFeeItem currentRule = getDetailed(id);

        Intake newEffectiveFrom =
                getIntake(request.effectiveFromIntakeId());

        ensureFutureBoundary(newEffectiveFrom);

        long currentFromSequence = currentRule
                .getEffectiveFromIntake()
                .getSequenceNumber();

        long newFromSequence =
                newEffectiveFrom.getSequenceNumber();

        if (newFromSequence <= currentFromSequence) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The new effective intake must be after the rule's starting intake"
            );
        }

        Intake previousEffectiveTo =
                currentRule.getEffectiveToIntake();

        if (previousEffectiveTo != null
            && newFromSequence
               >= previousEffectiveTo.getSequenceNumber()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The new effective intake must fall within the selected fee rule's range"
            );
        }

        validateCourseAvailableForIntake(
                currentRule.getCourseAcademicPeriod(),
                newEffectiveFrom
        );

        validateRange(
                currentRule.getCourseAcademicPeriod().getId(),
                currentRule.getFeeItem().getId(),
                newEffectiveFrom,
                previousEffectiveTo,
                currentRule.getId()
        );

        /*
         * Close the previous rule first.
         *
         * flush() is important once the PostgreSQL exclusion constraint
         * is enabled because the old row must be closed before inserting
         * the new overlapping starting point.
         */
        currentRule.setEffectiveToIntake(newEffectiveFrom);
        periodFeeItemRepository.saveAndFlush(currentRule);

        PeriodFeeItem newRule = new PeriodFeeItem();
        newRule.setCourseAcademicPeriod(
                currentRule.getCourseAcademicPeriod()
        );
        newRule.setFeeItem(currentRule.getFeeItem());
        newRule.setAmount(normalizeAmount(request.newAmount()));
        newRule.setEffectiveFromIntake(newEffectiveFrom);
        newRule.setEffectiveToIntake(previousEffectiveTo);
        newRule.setMandatory(currentRule.isMandatory());
        newRule.setDisplayOrder(currentRule.getDisplayOrder());

        return saveAndMap(newRule);
    }

    /**
     * Ends a fee rule at the supplied future intake.
     *
     * The end intake is exclusive.
     */
    @Transactional
    public PeriodFeeItemResponse stop(
            Long id,
            StopPeriodFeeItemRequest request
    ) {
        PeriodFeeItem item = getDetailed(id);

        Intake effectiveTo =
                getIntake(request.effectiveToIntakeId());

        ensureFutureBoundary(effectiveTo);

        long fromSequence = item
                .getEffectiveFromIntake()
                .getSequenceNumber();

        long toSequence = effectiveTo.getSequenceNumber();

        if (toSequence <= fromSequence) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The stopping intake must come after the starting intake"
            );
        }

        Intake currentEffectiveTo = item.getEffectiveToIntake();

        if (currentEffectiveTo != null
            && toSequence
               > currentEffectiveTo.getSequenceNumber()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The fee cannot be extended using the stop operation"
            );
        }

        item.setEffectiveToIntake(effectiveTo);

        return saveAndMap(item);
    }

    /**
     * Deletes only the latest open-ended rule when its starting intake
     * has not started.
     *
     * If the rule was created using changeAmount(), the previous version
     * is reopened.
     */
    @Transactional
    public void delete(Long id) {
        PeriodFeeItem item = getDetailed(id);

        ensureRuleHasNotStarted(item);

        if (item.getEffectiveToIntake() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only the latest open-ended future fee rule can be deleted"
            );
        }

        long startingSequence = item
                .getEffectiveFromIntake()
                .getSequenceNumber();

        var predecessor = periodFeeItemRepository.findPredecessor(
                item.getCourseAcademicPeriod().getId(),
                item.getFeeItem().getId(),
                startingSequence
        );

        /*
         * Delete and flush first so reopening the predecessor does not
         * temporarily overlap with the row being deleted.
         */
        periodFeeItemRepository.delete(item);
        periodFeeItemRepository.flush();

        predecessor.ifPresent(previousRule -> {
            previousRule.setEffectiveToIntake(null);
            periodFeeItemRepository.save(previousRule);
        });
    }

    @Transactional(readOnly = true)
    public EffectivePeriodFeesResponse getEffectiveFees(
            UUID courseAcademicPeriodUuid,
            Long intakeId
    ) {
        CourseAcademicPeriod courseAcademicPeriod =
                getCourseAcademicPeriod(courseAcademicPeriodUuid);

        Intake intake = getIntake(intakeId);

        validateCourseAvailableForIntake(
                courseAcademicPeriod,
                intake
        );

        List<PeriodFeeItem> effectiveFees =
                periodFeeItemRepository.findEffectiveFees(
                        courseAcademicPeriodUuid,
                        intake.getSequenceNumber()
                );

        List<PeriodFeeItemResponse> items = effectiveFees.stream()
                .map(PeriodFeeItemResponse::from)
                .toList();

        BigDecimal mandatoryTotal = effectiveFees.stream()
                .filter(PeriodFeeItem::isMandatory)
                .map(PeriodFeeItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal optionalTotal = effectiveFees.stream()
                .filter(item -> !item.isMandatory())
                .map(PeriodFeeItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal =
                mandatoryTotal.add(optionalTotal);

        var course = courseAcademicPeriod.getCourse();
        var academicPeriod =
                courseAcademicPeriod.getAcademicPeriod();

        return new EffectivePeriodFeesResponse(
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

                mandatoryTotal,
                optionalTotal,
                grandTotal,

                items
        );
    }

    @Transactional(readOnly = true)
    public List<PeriodFeeItemResponse> getHistory(
            UUID courseAcademicPeriodUuid,
            UUID feeItemUuid
    ) {
        /*
         * Validate the identifiers separately so invalid IDs return 404
         * rather than an unexplained empty array.
         */
        getCourseAcademicPeriod(courseAcademicPeriodUuid);
        getFeeItem(feeItemUuid);

        return periodFeeItemRepository
                .findHistory(
                        courseAcademicPeriodUuid,
                        feeItemUuid
                )
                .stream()
                .map(PeriodFeeItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getEffectiveTotalForEnrollment(
            Enrollment enrollment
    ) {
        var currentPeriod =
                enrollment.getCurrentCourseAcademicPeriod();

        var intake =
                enrollment.getIntake();

        List<PeriodFeeItem> items =
                periodFeeItemRepository.findEffectiveFees(
                        currentPeriod.getUuid(),
                        intake.getSequenceNumber()
                );

        if (items.isEmpty()) {
            throw new EntityNotFoundException(
                    "No fee items are configured for "
                    + enrollment.getCourse().getName()
                    + " / "
                    + currentPeriod.getAcademicPeriod().getCode()
                    + " / "
                    + intake.getName()
            );
        }

        return items.stream()
                .map(PeriodFeeItem::getAmount)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private Specification<PeriodFeeItem> buildSpecification(
            String search,
            UUID courseUuid,
            UUID courseAcademicPeriodUuid,
            UUID feeItemUuid,
            Long intakeSequence,
            Boolean mandatory
    ) {
        Specification<PeriodFeeItem> specification =
                (root, query, cb) -> cb.conjunction();

        if (courseUuid != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(
                            root.get("courseAcademicPeriod")
                                    .get("course")
                                    .get("uuid"),
                            courseUuid
                    )
            );
        }

        if (courseAcademicPeriodUuid != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(
                            root.get("courseAcademicPeriod")
                                    .get("uuid"),
                            courseAcademicPeriodUuid
                    )
            );
        }

        if (feeItemUuid != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(
                            root.get("feeItem").get("uuid"),
                            feeItemUuid
                    )
            );
        }

        if (mandatory != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(
                            root.get("mandatory"),
                            mandatory
                    )
            );
        }

        if (intakeSequence != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.and(
                            cb.lessThanOrEqualTo(
                                    root.get("effectiveFromIntake")
                                            .get("sequenceNumber"),
                                    intakeSequence
                            ),
                            cb.or(
                                    cb.isNull(
                                            root.get("effectiveToIntake")
                                    ),
                                    cb.greaterThan(
                                            root.get("effectiveToIntake")
                                                    .get("sequenceNumber"),
                                            intakeSequence
                                    )
                            )
                    )
            );
        }

        if (search != null && !search.isBlank()) {
            String pattern = "%"
                             + search.trim().toLowerCase(Locale.ROOT)
                             + "%";

            specification = specification.and(
                    (root, query, cb) -> {
                        var feeItem = root.join("feeItem", JoinType.INNER);
                        var courseAcademicPeriod =
                                root.join("courseAcademicPeriod", JoinType.INNER);
                        var course =
                                courseAcademicPeriod.join("course", JoinType.INNER);
                        var academicPeriod =
                                courseAcademicPeriod.join("academicPeriod", JoinType.INNER);

                        query.distinct(true);

                        return cb.or(
                                cb.like(cb.lower(feeItem.get("code")), pattern),
                                cb.like(cb.lower(feeItem.get("name")), pattern),
                                cb.like(cb.lower(course.get("code")), pattern),
                                cb.like(cb.lower(course.get("name")), pattern),
                                cb.like(cb.lower(academicPeriod.get("code")), pattern),
                                cb.like(cb.lower(academicPeriod.get("name")), pattern)
                        );
                    }
            );
        }

        return specification;
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(
                    Sort.Order.asc("displayOrder"),
                    Sort.Order.asc("feeItem.name"),
                    Sort.Order.asc("id")
            );
        }

        String[] components = sort.split(",");

        String field = components[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            field = "displayOrder";
        }

        Sort.Direction direction =
                components.length > 1
                && "desc".equalsIgnoreCase(
                        components[1].trim()
                )
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        return Sort.by(direction, field)
                .and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private void validateRange(
            Long courseAcademicPeriodId,
            Long feeItemId,
            Intake effectiveFrom,
            Intake effectiveTo,
            Long excludedId
    ) {
        long fromSequence =
                effectiveFrom.getSequenceNumber();

        long toSequence = effectiveTo != null
                ? effectiveTo.getSequenceNumber()
                : OPEN_ENDED_SEQUENCE;

        if (effectiveTo != null
            && toSequence <= fromSequence) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The effective-to intake must come after the effective-from intake"
            );
        }

        long overlappingRules =
                periodFeeItemRepository.countOverlappingRules(
                        courseAcademicPeriodId,
                        feeItemId,
                        fromSequence,
                        toSequence,
                        excludedId
                );

        if (overlappingRules > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This fee item already has an overlapping intake range for the selected course period"
            );
        }
    }

    private void validateCourseAvailableForIntake(
            CourseAcademicPeriod courseAcademicPeriod,
            Intake intake
    ) {
        Long courseId =
                courseAcademicPeriod.getCourse().getId();

        boolean available =
                intakeCourseRepository
                        .existsByIntake_IdAndCourse_Id(
                                intake.getId(),
                                courseId
                        );

        if (!available) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected course is not offered in intake "
                    + intake.getName()
            );
        }
    }

    private void ensureRuleHasNotStarted(
            PeriodFeeItem item
    ) {
        LocalDate today =
                LocalDate.now(clock);

        LocalDate startingDate = item
                .getEffectiveFromIntake()
                .getStartDate();

        if (!startingDate.isAfter(today)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This fee rule has already become effective and cannot be edited or deleted directly"
            );
        }
    }

    private void ensureFutureBoundary(Intake intake) {
        LocalDate today =
                LocalDate.now(clock);

        if (!intake.getStartDate().isAfter(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fee changes must take effect from a future intake"
            );
        }
    }

    private PeriodFeeItemResponse saveAndMap(
            PeriodFeeItem item
    ) {
        try {
            PeriodFeeItem saved =
                    periodFeeItemRepository.saveAndFlush(item);

            return PeriodFeeItemResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The fee rule conflicts with an existing fee configuration",
                    exception
            );
        }
    }

    private PeriodFeeItem getDetailed(Long id) {
        return periodFeeItemRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Period fee item not found"
                ));
    }

    private FeeItem getFeeItem(UUID uuid) {
        return feeItemRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fee item not found"
                ));
    }

    private CourseAcademicPeriod getCourseAcademicPeriod(
            UUID uuid
    ) {
        return courseAcademicPeriodRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Course academic period not found"
                ));
    }

    private Intake getIntake(Long id) {
        return intakeRepository.findById(id)
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
                java.math.RoundingMode.HALF_UP
        );
    }
}
