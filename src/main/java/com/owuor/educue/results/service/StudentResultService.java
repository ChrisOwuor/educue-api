package com.owuor.educue.results.service;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.results.dto.*;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.results.repository.StudentResultSpecification;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDateTime;
import java.util.*;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class StudentResultService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentResultRepository resultRepository;
    private final com.owuor.educue.students.repository.StudentUnitRegistrationRepository registrationRepository;

    @Transactional
    public Page<StudentResultResponse> getResults(StudentResultFilterRequest filter, Pageable pageable) {
        return resultRepository.findAll(
                StudentResultSpecification.withFilters(
                        filter.getSearch(),
                        filter.getCourseId(),
                        filter.getCourseAcademicPeriodId(),
                        filter.getCourseUnitPlacementId(),
                        filter.getStatus(),
                        filter.getPassed()
                ),
                pageable
        ).map(this::toResponse);
    }

    private StudentResultResponse toResponse(StudentResult result) {
        StudentUnitRegistration registration = result.getStudentUnitRegistration();
        Enrollment enrollment = registration.getEnrollment();
        var placement = registration.getCourseUnitPlacement();

        return StudentResultResponse.builder()
                .resultId(result.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getFullName())
                .admissionNumber(enrollment.getStudent().getAdmissionNumber())
                .unitCode(placement.getUnit().getCode())
                .unitName(placement.getUnit().getName())
                .course(enrollment.getCourse().getName())
                .academicPeriod(placement.getCourseAcademicPeriod().getAcademicPeriod().getName())
                .courseAcademicPeriodId(placement.getCourseAcademicPeriod().getId())
                .attemptType(registration.getAttemptType().name())
                .activeFurtherAttempt(registrationRepository.existsActiveFurtherAttempt(enrollment.getId(), placement.getId()))
                .caMarks(result.getCaMarks())
                .examMarks(result.getExamMarks())
                .totalMarks(result.getTotalMarks())
                .grade(result.getGrade())
                .passed(result.isPassed())
                .status(result.getStatus().name())
                .remarks(result.getRemarks())
                .recordedByName(result.getRecordedBy() != null ? result.getRecordedBy().getFullName() : null)
                .approvedByName(result.getApprovedBy() != null ? result.getApprovedBy().getFullName() : null)
                .approvedAt(result.getApprovedAt())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }


    @Transactional
    public List<StudentResultResponse> getMyResults(Long userId, Long courseAcademicPeriodId, String outcome) {
        var stream = resultRepository
                .findByStudentUnitRegistrationEnrollmentStudentUserIdOrderByStudentUnitRegistrationCourseUnitPlacementCourseAcademicPeriodPosition(userId)
                .stream().filter(result -> result.getStatus() == ResultStatus.APPROVED || result.getStatus() == ResultStatus.RELEASED);
        if (courseAcademicPeriodId != null)
            stream = stream.filter(result -> result.getStudentUnitRegistration().getCourseUnitPlacement().getCourseAcademicPeriod().getId().equals(courseAcademicPeriodId));
        if (outcome != null && !outcome.isBlank()) {
            boolean passed = switch (outcome.toUpperCase()) {
                case "PASSED" -> true;
                case "FAILED" -> false;
                default -> throw new IllegalArgumentException("Outcome must be PASSED or FAILED");
            };
            stream = stream.filter(result -> result.isPassed() == passed);
        }
        return stream.map(this::toResponse).toList();
    }


    @Transactional
    public void updateBatchStatus(BatchApprovalRequest request, User hodUser) {
        if (request.resultIds() == null || request.resultIds().isEmpty()) {
            return;
        }

        // Retrieve all targeted results
        List<StudentResult> results = resultRepository.findAllById(request.resultIds());

        for (StudentResult result : results) {

            if (request.status() == ResultStatus.APPROVED) {
                result.approve(hodUser);
            } else {
                result.withhold("Results withheld you will be contacted by the hod ");
            }
        }

        // Persist modifications back into DB
        resultRepository.saveAll(results);
    }

    @Transactional
    public MyPeriodResultsResponse getMyCurrentPeriodResults(
            Long userId,
            String outcome
    ) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Active enrollment not found."
                        )
                );

        var currentCoursePeriod =
                enrollment.getCurrentCourseAcademicPeriod();

        List<StudentResult> releasedResults =
                resultRepository
                        .findByStudentUnitRegistrationEnrollmentStudentUserIdOrderByStudentUnitRegistrationCourseUnitPlacementCourseAcademicPeriodPosition(
                                userId
                        )
                        .stream()
                        .filter(result ->
                                result.getStatus() ==
                                ResultStatus.APPROVED
                        )
                        .filter(result ->
                                result
                                        .getStudentUnitRegistration()
                                        .getCourseUnitPlacement()
                                        .getCourseAcademicPeriod()
                                        .getId()
                                        .equals(
                                                currentCoursePeriod.getId()
                                        )
                        )
                        .toList();

        /*
         * A student can have multiple attempts for the same
         * course-unit placement.
         *
         * Count the unit only once by selecting the latest
         * released result.
         */
        Map<Long, StudentResult> latestResultByPlacement =
                releasedResults.stream()
                        .collect(Collectors.toMap(
                                result ->
                                        result
                                                .getStudentUnitRegistration()
                                                .getCourseUnitPlacement()
                                                .getId(),

                                Function.identity(),

                                (first, second) ->
                                        first.getId() >= second.getId()
                                                ? first
                                                : second,

                                LinkedHashMap::new
                        ));

        List<StudentResult> effectiveResults =
                new ArrayList<>(
                        latestResultByPlacement.values()
                );

        /*
         * Calculate the summary before applying the outcome filter.
         *
         * Otherwise selecting "FAILED" on the frontend would
         * incorrectly recalculate the average from failed units only.
         */
        MyPeriodResultsResponse.PeriodSummary summary =
                calculatePeriodSummary(
                        currentCoursePeriod,
                        effectiveResults
                );

        Stream<StudentResult> displayedResults =
                effectiveResults.stream();

        if (outcome != null && !outcome.isBlank()) {
            boolean passed = switch (
                    outcome.toUpperCase()
                    ) {
                case "PASSED" -> true;
                case "FAILED" -> false;

                default -> throw new IllegalArgumentException(
                        "Outcome must be PASSED or FAILED."
                );
            };

            displayedResults = displayedResults.filter(
                    result -> result.isPassed() == passed
            );
        }

        List<StudentResultResponse> responses =
                displayedResults
                        .map(this::toResponse)
                        .toList();

        return new MyPeriodResultsResponse(
                summary,
                responses
        );
    }

    private MyPeriodResultsResponse.PeriodSummary
    calculatePeriodSummary(
            CourseAcademicPeriod coursePeriod,
            List<StudentResult> results
    ) {
        BigDecimal totalWeightedMarks =
                BigDecimal.ZERO;

        int totalCredits = 0;
        int totalUnits = 0;
        int passedUnits = 0;
        int failedUnits = 0;

        for (StudentResult result : results) {
            BigDecimal totalMarks =
                    result.getTotalMarks();

            if (totalMarks == null) {
                continue;
            }

            var unit = result
                    .getStudentUnitRegistration()
                    .getCourseUnitPlacement()
                    .getUnit();

            Integer creditHours =
                    unit.getCreditHours();

            if (creditHours == null || creditHours <= 0) {
                throw new IllegalStateException(
                        "Unit " + unit.getCode() +
                        " does not have valid credit hours."
                );
            }

            BigDecimal weightedMarks =
                    totalMarks.multiply(
                            BigDecimal.valueOf(creditHours)
                    );

            totalWeightedMarks =
                    totalWeightedMarks.add(
                            weightedMarks
                    );

            totalCredits += creditHours;
            totalUnits++;

            if (result.isPassed()) {
                passedUnits++;
            } else {
                failedUnits++;
            }
        }

        BigDecimal average =
                totalCredits == 0
                        ? null
                        : totalWeightedMarks.divide(
                        BigDecimal.valueOf(
                                totalCredits
                        ),
                        2,
                        RoundingMode.HALF_UP
                );

        return new MyPeriodResultsResponse.PeriodSummary(
                coursePeriod.getId(),
                coursePeriod
                        .getAcademicPeriod()
                        .getCode(),
                coursePeriod
                        .getAcademicPeriod()
                        .getName(),
                average,
                totalWeightedMarks,
                totalUnits,
                totalCredits,
                passedUnits,
                failedUnits
        );
    }


    @Transactional
    public MyAcademicResultsResponse getMyAcademicResults(
            Long userId,
            Long selectedCourseAcademicPeriodId,
            String outcome
    ) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Active enrollment not found."
                        )
                );

        var currentCoursePeriod =
                enrollment.getCurrentCourseAcademicPeriod();



        Long currentCourseId = currentCoursePeriod
                .getCourse()
                .getId();

        Integer currentPosition =
                currentCoursePeriod.getPosition();

        /*
         * Load the student's results and retain only:
         *
         * 1. Results for this enrollment.
         * 2. Results for the current course.
         * 3. Results up to the current academic period.
         * 4. Results visible to the student.
         */
        List<StudentResult> visibleResults =
                resultRepository
                        .findByStudentUnitRegistrationEnrollmentStudentUserIdOrderByStudentUnitRegistrationCourseUnitPlacementCourseAcademicPeriodPosition(
                                userId
                        )
                        .stream()
                        .filter(this::isVisibleToStudent)
                        .filter(result ->
                                Objects.equals(
                                        result
                                                .getStudentUnitRegistration()
                                                .getEnrollment()
                                                .getId(),
                                        enrollment.getId()
                                )
                        )
                        .filter(result ->
                                Objects.equals(
                                        result
                                                .getStudentUnitRegistration()
                                                .getCourseUnitPlacement()
                                                .getCourseAcademicPeriod()
                                                .getCourse()
                                                .getId(),
                                        currentCourseId
                                )
                        )
                        .filter(result -> {
                            Integer resultPeriodPosition =
                                    result
                                            .getStudentUnitRegistration()
                                            .getCourseUnitPlacement()
                                            .getCourseAcademicPeriod()
                                            .getPosition();

                            return resultPeriodPosition <= currentPosition;
                        })
                        .toList();

        Map<Long, StudentResult> effectiveResultByPlacement =
                visibleResults.stream()
                        .collect(Collectors.toMap(
                                result ->
                                        result
                                                .getStudentUnitRegistration()
                                                .getCourseUnitPlacement()
                                                .getId(),

                                Function.identity(),

                                this::selectLatestResult,

                                LinkedHashMap::new
                        ));

        List<StudentResult> effectiveResults =
                effectiveResultByPlacement
                        .values()
                        .stream()
                        .sorted(
                                Comparator
                                        .comparing(this::periodPosition)
                                        .thenComparing(this::unitCode)
                        )
                        .toList();

        /*
         * Keep all attempts for display.
         */
        List<StudentResult> displayResults =
                visibleResults.stream()
                        .sorted(
                                Comparator
                                        .comparing(this::periodPosition)
                                        .thenComparing(this::unitCode)
                                        .thenComparing(StudentResult::getId)
                        )
                        .toList();

        Map<Integer, List<StudentResult>> displayResultsByPeriod =
                displayResults.stream()
                        .collect(Collectors.groupingBy(
                                this::periodPosition,
                                TreeMap::new,
                                Collectors.toList()
                        ));

        Map<Integer, List<StudentResult>> calculationResultsByPeriod =
                effectiveResults.stream()
                        .collect(Collectors.groupingBy(
                                this::periodPosition,
                                TreeMap::new,
                                Collectors.toList()
                        ));

        List<MyAcademicResultsResponse.AcademicPeriodResult>
                periodResponses = new ArrayList<>();

        BigDecimal runningPeriodAverageTotal =
                BigDecimal.ZERO;

        int periodsWithResults = 0;

        for (Map.Entry<Integer, List<StudentResult>> entry
                : displayResultsByPeriod.entrySet()) {

            Integer position =
                    entry.getKey();

            List<StudentResult> periodDisplayResults =
                    entry.getValue();

            List<StudentResult> periodCalculationResults =
                    calculationResultsByPeriod.getOrDefault(
                            position,
                            List.of()
                    );

            if (periodDisplayResults.isEmpty()) {
                continue;
            }

            var courseAcademicPeriod =
                    periodDisplayResults
                            .getFirst()
                            .getStudentUnitRegistration()
                            .getCourseUnitPlacement()
                            .getCourseAcademicPeriod();

            /*
             * The calculation list contains one effective
             * result per unit.
             *
             * calculatePeriodAverage also excludes failed
             * effective results.
             */
            PeriodCalculation periodCalculation =
                    calculatePeriodAverage(
                            periodCalculationResults
                    );
            String recommendation =
                    determinePeriodRecommendation(
                            periodCalculationResults
                    );

            BigDecimal currentAverage =
                    periodCalculation.average();

            BigDecimal cumulativeAverage = null;

            if (currentAverage != null) {
                runningPeriodAverageTotal =
                        runningPeriodAverageTotal.add(
                                currentAverage
                        );

                periodsWithResults++;

                cumulativeAverage =
                        runningPeriodAverageTotal.divide(
                                BigDecimal.valueOf(
                                        periodsWithResults
                                ),
                                6,
                                RoundingMode.HALF_UP
                        );
            }

            /*
             * Use all historical attempts for display.
             */
            Stream<StudentResult> displayedUnits =
                    periodDisplayResults.stream();

            if (outcome != null && !outcome.isBlank()) {
                boolean passed = switch (
                        outcome.toUpperCase()
                        ) {
                    case "PASSED" -> true;
                    case "FAILED" -> false;

                    default -> throw new IllegalArgumentException(
                            "Outcome must be PASSED or FAILED."
                    );
                };

                displayedUnits =
                        displayedUnits.filter(
                                result ->
                                        result.isPassed() == passed
                        );
            }



            List<StudentResultResponse> unitResponses =
                    displayedUnits
                            .map(this::toAcademicResultsResponse)
                            .toList();

            var academicPeriod =
                    courseAcademicPeriod.getAcademicPeriod();

            periodResponses.add(
                    new MyAcademicResultsResponse
                            .AcademicPeriodResult(
                            courseAcademicPeriod.getId(),

                            academicPeriod.getCode(),
                            academicPeriod.getName(),

                            academicPeriod.getYearNumber(),
                            academicPeriod.getPeriodNumber(),
                            courseAcademicPeriod.getPosition(),

                            roundForDisplay(currentAverage),
                            roundForDisplay(cumulativeAverage),

                            recommendation,

                            unitResponses
                    )
            );
        }

        /*
         * The cumulative values must be calculated before filtering
         * the returned periods.
         *
         * This ensures that selecting Y2S1 still shows its correct
         * cumulative average from every preceding period.
         */
        if (selectedCourseAcademicPeriodId != null) {
            periodResponses = periodResponses.stream()
                    .filter(period ->
                            Objects.equals(
                                    period.courseAcademicPeriodId(),
                                    selectedCourseAcademicPeriodId
                            )
                    )
                    .toList();
        }

        return new MyAcademicResultsResponse(
                periodResponses
        );
    }

    /** Authoritative cumulative used by both transcripts and graduation snapshots. */
    @Transactional
    public BigDecimal calculateFinalCumulativeAverage(Long enrollmentId) {
        var effective = resultRepository.findByStudentUnitRegistrationEnrollmentId(enrollmentId).stream()
                .filter(result -> result.getStatus() == ResultStatus.APPROVED)
                .collect(Collectors.toMap(
                        result -> result.getStudentUnitRegistration().getCourseUnitPlacement().getId(),
                        Function.identity(), this::selectLatestResult, LinkedHashMap::new))
                .values().stream()
                .collect(Collectors.groupingBy(this::periodPosition, TreeMap::new, Collectors.toList()));
        BigDecimal total = BigDecimal.ZERO; int periods = 0;
        for (var period : effective.values()) {
            BigDecimal average = calculatePeriodAverage(period).average();
            if (average != null) { total = total.add(average); periods++; }
        }
        return periods == 0 ? null : total.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
    }

    private StudentResultResponse toAcademicResultsResponse(
            StudentResult result
    ) {
        StudentUnitRegistration registration =
                result.getStudentUnitRegistration();

        Enrollment enrollment =
                registration.getEnrollment();

        var placement =
                registration.getCourseUnitPlacement();

        var courseAcademicPeriod =
                placement.getCourseAcademicPeriod();

        var academicPeriod =
                courseAcademicPeriod.getAcademicPeriod();

        Integer creditHours =
                placement
                        .getUnit()
                        .getCreditHours();

        BigDecimal weightedMarks =
                result.getTotalMarks() == null ||
                creditHours == null
                        ? null
                        : result
                        .getTotalMarks()
                        .multiply(
                                BigDecimal.valueOf(
                                        creditHours
                                )
                        );

        return StudentResultResponse.builder()
                .resultId(result.getId())
                .studentId(
                        enrollment.getStudent().getId()
                )
                .studentName(
                        enrollment.getStudent().getFullName()
                )
                .admissionNumber(
                        enrollment
                                .getStudent()
                                .getAdmissionNumber()
                )
                .unitCode(
                        placement.getUnit().getCode()
                )
                .unitName(
                        placement.getUnit().getName()
                )
                .course(
                        enrollment.getCourse().getName()
                )
                .academicPeriod(
                        academicPeriod.getName()
                )
                .academicPeriodCode(
                        academicPeriod.getCode()
                )
                .courseAcademicPeriodId(
                        courseAcademicPeriod.getId()
                )
                .creditHours(creditHours)
                .weightedMarks(weightedMarks)
                .attemptType(
                        registration
                                .getAttemptType()
                                .name()
                )
                .activeFurtherAttempt(
                        registrationRepository
                                .existsActiveFurtherAttempt(
                                        enrollment.getId(),
                                        placement.getId()
                                )
                )
                .caMarks(result.getCaMarks())
                .examMarks(result.getExamMarks())
                .totalMarks(result.getTotalMarks())
                .grade(result.getGrade())
                .passed(result.isPassed())
                .status(result.getStatus().name())
                .remarks(result.getRemarks())
                .recordedByName(
                        result.getRecordedBy() == null
                                ? null
                                : result
                                .getRecordedBy()
                                .getFullName()
                )
                .approvedByName(
                        result.getApprovedBy() == null
                                ? null
                                : result
                                .getApprovedBy()
                                .getFullName()
                )
                .approvedAt(result.getApprovedAt())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    private PeriodCalculation calculatePeriodAverage(
            List<StudentResult> results
    ) {
        BigDecimal totalWeightedMarks =
                BigDecimal.ZERO;

        int totalCredits = 0;

        for (StudentResult result : results) {
            BigDecimal totalMarks =
                    result.getTotalMarks();

            if (totalMarks == null) {
                continue;
            }

            var unit = result
                    .getStudentUnitRegistration()
                    .getCourseUnitPlacement()
                    .getUnit();

            Integer creditHours =
                    unit.getCreditHours();

            if (creditHours == null || creditHours <= 0) {
                throw new IllegalStateException(
                        "Unit " +
                        unit.getCode() +
                        " does not have valid credit hours."
                );
            }

            BigDecimal weightedMarks =
                    totalMarks.multiply(
                            BigDecimal.valueOf(
                                    creditHours
                            )
                    );

            totalWeightedMarks =
                    totalWeightedMarks.add(
                            weightedMarks
                    );

            // Every approved graded attempt contributes to the cumulative
            // average. Pass/fail remains a separate graduation blocker.
            totalCredits += creditHours;
        }

        BigDecimal average =
                totalCredits == 0
                        ? null
                        : totalWeightedMarks.divide(
                        BigDecimal.valueOf(
                                totalCredits
                        ),
                        6,
                        RoundingMode.HALF_UP
                );

        return new PeriodCalculation(
                average,
                totalWeightedMarks,
                totalCredits
        );
    }

    private record PeriodCalculation(
            BigDecimal average,
            BigDecimal totalWeightedMarks,
            int totalCredits
    ) {
    }
    private BigDecimal roundForDisplay(
            BigDecimal value
    ) {
        return value == null
                ? null
                : value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
    private StudentResult selectLatestResult(
            StudentResult first,
            StudentResult second
    ) {
        return first.getId() >= second.getId()
                ? first
                : second;
    }

    private boolean isVisibleToStudent(
            StudentResult result
    ) {
        return result.getStatus() ==
               ResultStatus.APPROVED;

    }
    private String determinePeriodRecommendation(
            List<StudentResult> effectiveResults
    ) {
        if (effectiveResults.isEmpty()) {
            return "RESULTS NOT YET AVAILABLE";
        }

        boolean hasIncompleteResult =
                effectiveResults.stream()
                        .anyMatch(result ->
                                result.getTotalMarks() == null
                        );

        if (hasIncompleteResult) {
            return "RESULTS INCOMPLETE - AWAITING FINALIZATION";
        }

        boolean hasFailedUnit =
                effectiveResults.stream()
                        .anyMatch(result ->
                                !result.isPassed()
                        );

        if (hasFailedUnit) {
            return "OUTSTANDING FAILED UNIT(S) - RESIT OR RETAKE REQUIRED";
        }

        return "CLEARED TO PROCEED TO THE NEXT ACADEMIC PERIOD";
    }

    private Integer periodPosition(
            StudentResult result
    ) {
        return result
                .getStudentUnitRegistration()
                .getCourseUnitPlacement()
                .getCourseAcademicPeriod()
                .getPosition();
    }

    private String unitCode(
            StudentResult result
    ) {
        return result
                .getStudentUnitRegistration()
                .getCourseUnitPlacement()
                .getUnit()
                .getCode();
    }
}
