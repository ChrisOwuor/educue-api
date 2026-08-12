package com.owuor.educue.admissions.service;

import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.admissions.dto.CourseIntakeConfigurationResponse;
import com.owuor.educue.admissions.dto.CreateIntakeRequest;
import com.owuor.educue.admissions.dto.IntakeDetailResponse;
import com.owuor.educue.admissions.dto.IntakeResponse;
import com.owuor.educue.admissions.entity.CourseIntakeConfiguration;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.entity.IntakeCourse;
import com.owuor.educue.admissions.enums.IntakeStatus;
import com.owuor.educue.admissions.repository.IntakeCourseRepository;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.admissions.repository.CourseIntakeConfigurationRepository;
import com.owuor.educue.finance.repository.PeriodFeeItemRepository;
import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntakeService {

    private final IntakeRepository intakeRepository;
    private final IntakeCourseRepository intakeCourseRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final CourseUnitPlacementRepository courseUnitPlacementRepository;
    private final PeriodFeeItemRepository periodFeeItemRepository;
    private final CourseIntakeConfigurationRepository configurationRepository;


    @Transactional
    public IntakeResponse create(CreateIntakeRequest request) {
        String intakeName = request.name().trim();

        if (intakeRepository.existsByNameIgnoreCase(intakeName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An intake with this name already exists"
            );
        }

        if (request.applicationDeadline().isAfter(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Application deadline cannot be after the intake start date"
            );
        }

        AcademicYear academicYear = academicYearRepository
                .findByUuid(request.academicYearUuid())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Academic year not found"
                ));

        if (!academicYear.isActive() || academicYear.isClosed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected academic year is not open"
            );
        }

        if (request.startDate().isBefore(academicYear.getStartDate())
            || request.startDate().isAfter(academicYear.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Intake start date must fall within the selected academic year"
            );
        }

        Intake intake = new Intake();
        intake.setUuid(UUID.randomUUID());
        intake.setName(intakeName);
        intake.setAcademicYear(academicYear);
        intake.setStartDate(request.startDate());
        intake.setApplicationDeadline(request.applicationDeadline());
        intake.setStatus(IntakeStatus.DRAFT);

        // PostgreSQL/Hibernate generates sequenceNumber here.
        Intake savedIntake = intakeRepository.saveAndFlush(intake);

        return IntakeResponse.from(savedIntake, List.of());
    }

    public IntakeResponse getById(Long id) {
        Intake intake = findEntity(id);
        return IntakeResponse.from(intake, intakeCourseRepository.findByIntakeId(id));
    }

    @Transactional(readOnly = true)
    public IntakeDetailResponse getDetail(Long id) {
        Intake intake = findEntity(id);
        List<IntakeCourse> attached = intakeCourseRepository.findByIntakeId(id);
        List<CourseIntakeConfigurationResponse> configurations = configurationRepository.findByIntakeId(id).stream()
                .map(config -> configurationView(intake, config.getCourse(), config,
                        intakeCourseRepository.existsByIntakeIdAndCourseId(id, config.getCourse().getId())))
                .toList();
        boolean publishable = configurations.stream().anyMatch(CourseIntakeConfigurationResponse::ready);
        return new IntakeDetailResponse(IntakeResponse.from(intake, attached), configurations, publishable);
    }

    @Transactional(readOnly = true)
    public List<IntakeResponse> getAll() {
        return intakeRepository.findAll().stream()
                .map(intake -> IntakeResponse.from(intake, intakeCourseRepository.findByIntakeId(intake.getId())))
                .toList();
    }

    // The endpoint the public /apply page actually calls. Filters to
    // intakes whose deadline hasn't passed - "can people click apply
    // right now" is the real question, not just a status label that
    // someone might forget to update.
    @Transactional(readOnly = true)
    public List<IntakeResponse> getOpenForApplications() {
        return intakeRepository.findByApplicationDeadlineGreaterThanEqual(LocalDate.now())
                .stream()
                .filter(intake -> intake.getStatus() == IntakeStatus.OPEN)
                .map(intake -> IntakeResponse.from(intake, intakeCourseRepository.findByIntakeId(intake.getId())))
                // Only return intakes that actually have at least one course
                // attached - protects the apply page from showing a window
                // with nothing to apply for, even if one somehow got created
                // with zero courses through a different path.
                .filter(response -> !response.courses().isEmpty())
                .toList();
    }

    @Transactional
    public void addCourseToIntake(Long intakeId, Long courseId) {
        Intake intake = findEntity(intakeId);
        requireDraft(intake);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        CourseIntakeConfigurationResponse view = configurationView(intake, course,
                configurationRepository.findByIntakeIdAndCourseId(intakeId, courseId).orElse(null), false);
        if (!view.ready()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Academic units and first-period fees must both be configured and confirmed before adding this course");
        }

        if (intakeCourseRepository.existsByIntakeIdAndCourseId(intakeId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This course is already open for this intake");
        }

        IntakeCourse ic = new IntakeCourse();
        ic.setIntake(intake);
        ic.setCourse(course);
        intakeCourseRepository.save(ic);
    }

    @Transactional
    public void removeCourseFromIntake(Long intakeId, Long courseId) {
        requireDraft(findEntity(intakeId));
        List<IntakeCourse> all = intakeCourseRepository.findByIntakeId(intakeId);
        IntakeCourse match = all.stream()
                .filter(ic -> ic.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("This course is not attached to this intake"));

        intakeCourseRepository.delete(match);
    }

    @Transactional
    public IntakeResponse close(Long id) {
        Intake intake = findEntity(id);
        intake.setStatus(IntakeStatus.CLOSED);
        intake = intakeRepository.save(intake);
        return IntakeResponse.from(intake, intakeCourseRepository.findByIntakeId(id));
    }

    @Transactional
    public IntakeDetailResponse confirmAcademic(Long intakeId, Long courseId, boolean confirmed) {
        Intake intake = findEntity(intakeId);
        requireDraft(intake);
        Course course = findCourse(courseId);
        if (confirmed) {
            var periods = courseAcademicPeriodRepository.findByCourseIdOrderByPosition(courseId);
            if (periods.isEmpty() || periods.stream().anyMatch(period ->
                    courseUnitPlacementRepository.findEffectiveForPeriodAndIntakeSequence(
                            period.getId(), intake.getSequenceNumber()).isEmpty())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Configure units for every course period before confirming academics");
            }
        }
        CourseIntakeConfiguration config = findOrCreateConfiguration(intake, course);
        config.setAcademicConfirmed(confirmed);
        config.setAcademicConfirmedAt(confirmed ? LocalDateTime.now() : null);
        configurationRepository.save(config);
        return getDetail(intakeId);
    }

    @Transactional
    public IntakeDetailResponse confirmFees(Long intakeId, Long courseId, boolean confirmed) {
        Intake intake = findEntity(intakeId);
        requireDraft(intake);
        Course course = findCourse(courseId);
        if (confirmed) {
            var periods = courseAcademicPeriodRepository.findByCourseIdOrderByPosition(courseId);
            if (periods.isEmpty() || periods.stream().anyMatch(period ->
                    periodFeeItemRepository.findEffectiveFees(
                            period.getUuid(), intake.getSequenceNumber()).isEmpty())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Configure fees for every course period before confirming finance");
            }
        }
        CourseIntakeConfiguration config = findOrCreateConfiguration(intake, course);
        config.setFeeConfirmed(confirmed);
        config.setFeeConfirmedAt(confirmed ? LocalDateTime.now() : null);
        configurationRepository.save(config);
        return getDetail(intakeId);
    }

    @Transactional
    public IntakeDetailResponse publish(Long id) {
        Intake intake = findEntity(id);
        requireDraft(intake);
        List<CourseIntakeConfiguration> readyConfigurations = configurationRepository.findByIntakeId(id).stream()
                .filter(config -> configurationView(intake, config.getCourse(), config, false).ready())
                .toList();
        if (readyConfigurations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Confirm first-period academic and fee configuration for at least one course before publishing");
        }
        List<IntakeCourse> intakeCourses = readyConfigurations.stream()
                .filter(config -> !intakeCourseRepository.existsByIntakeIdAndCourseId(id, config.getCourse().getId()))
                .map(config -> {
                    IntakeCourse intakeCourse = new IntakeCourse();
                    intakeCourse.setIntake(intake);
                    intakeCourse.setCourse(config.getCourse());
                    return intakeCourse;
                })
                .toList();
        intakeCourseRepository.saveAllAndFlush(intakeCourses);
        intake.setStatus(IntakeStatus.OPEN);
        intakeRepository.save(intake);
        return getDetail(id);
    }

    private CourseIntakeConfigurationResponse configurationView(Intake intake, Course course,
            CourseIntakeConfiguration config, boolean attached) {
        CourseAcademicPeriod firstPeriod = courseAcademicPeriodRepository
                .findFirstByCourseIdOrderByPositionAsc(course.getId()).orElse(null);
        long units = firstPeriod == null ? 0 : courseUnitPlacementRepository
                .findEffectiveForPeriodAndIntakeSequence(firstPeriod.getId(), intake.getSequenceNumber()).size();
        long fees = firstPeriod == null ? 0 : periodFeeItemRepository
                .findEffectiveFees(firstPeriod.getUuid(), intake.getSequenceNumber()).size();
        boolean academicConfirmed = config != null && config.isAcademicConfirmed();
        boolean feeConfirmed = config != null && config.isFeeConfirmed();
        boolean ready = units > 0 && fees > 0 && academicConfirmed && feeConfirmed;
        return new CourseIntakeConfigurationResponse(
                intake.getId(), intake.getName(),
                course.getId(), course.getUuid(), course.getCode(), course.getName(),
                firstPeriod == null ? null : firstPeriod.getUuid(),
                firstPeriod == null ? null : firstPeriod.getAcademicPeriod().getCode(),
                units, fees, units > 0, fees > 0, academicConfirmed, feeConfirmed, attached, ready);
    }

    private CourseIntakeConfiguration findOrCreateConfiguration(Intake intake, Course course) {
        return configurationRepository.findByIntakeIdAndCourseId(intake.getId(), course.getId())
                .orElseGet(() -> {
                    CourseIntakeConfiguration configuration = new CourseIntakeConfiguration();
                    configuration.setIntake(intake);
                    configuration.setCourse(course);
                    return configuration;
                });
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
    }

    private void requireDraft(Intake intake) {
        if (intake.getStatus() != IntakeStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft intakes can be configured");
        }
    }

    private Intake findEntity(Long id) {
        return intakeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Intake not found"));
    }
}
