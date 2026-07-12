package com.owuor.educue.admissions.service;

import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.admissions.dto.CreateIntakeRequest;
import com.owuor.educue.admissions.dto.IntakeResponse;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.entity.IntakeCourse;
import com.owuor.educue.admissions.enums.IntakeStatus;
import com.owuor.educue.admissions.repository.IntakeCourseRepository;
import com.owuor.educue.admissions.repository.IntakeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntakeService {

    private final IntakeRepository intakeRepository;
    private final IntakeCourseRepository intakeCourseRepository;
    private final CourseRepository courseRepository;

    public IntakeResponse create(CreateIntakeRequest request) {
        if (intakeRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An intake with this name already exists");
        }

        if (request.applicationDeadline().isAfter(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Application deadline cannot be after the intake start date"
            );
        }

        // Validate every course id up front, BEFORE creating anything -
        // fail loudly on a bad id rather than silently creating an Intake
        // with only some of its intended courses attached.
        List<Course> courses = request.courseIds().stream()
                .map(id -> courseRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Course not found: " + id)))
                .toList();

        List<Course> inactiveCourses = courses.stream().filter(c -> !c.isActive()).toList();
        if (!inactiveCourses.isEmpty()) {
            String names = inactiveCourses.stream().map(Course::getName).reduce((a, b) -> a + ", " + b).orElse("");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot attach inactive course(s) to an intake: " + names
            );
        }

        Intake intake = new Intake();
        intake.setUuid(UUID.randomUUID());
        intake.setName(request.name());
        intake.setStartDate(request.startDate());
        intake.setApplicationDeadline(request.applicationDeadline());
        intake.setStatus(
                request.applicationDeadline().isBefore(LocalDate.now())
                        ? IntakeStatus.CLOSED
                        : IntakeStatus.OPEN
        );
        intake = intakeRepository.save(intake);

        Intake finalIntake = intake;
        List<IntakeCourse> intakeCourses = courses.stream()
                .map(course -> {
                    IntakeCourse ic = new IntakeCourse();
                    ic.setIntake(finalIntake);
                    ic.setCourse(course);
                    return ic;
                })
                .toList();
        intakeCourseRepository.saveAll(intakeCourses);

        return IntakeResponse.from(intake, intakeCourses);
    }

    public IntakeResponse getById(Long id) {
        Intake intake = findEntity(id);
        return IntakeResponse.from(intake, intakeCourseRepository.findByIntakeId(id));
    }

    public List<IntakeResponse> getAll() {
        return intakeRepository.findAll().stream()
                .map(intake -> IntakeResponse.from(intake, intakeCourseRepository.findByIntakeId(intake.getId())))
                .toList();
    }

    // The endpoint the public /apply page actually calls. Filters to
    // intakes whose deadline hasn't passed - "can people click apply
    // right now" is the real question, not just a status label that
    // someone might forget to update.
    public List<IntakeResponse> getOpenForApplications() {
        return intakeRepository.findByApplicationDeadlineGreaterThanEqual(LocalDate.now())
                .stream()
                .filter(intake -> intake.getStatus() != IntakeStatus.CLOSED)
                .map(intake -> IntakeResponse.from(intake, intakeCourseRepository.findByIntakeId(intake.getId())))
                // Only return intakes that actually have at least one course
                // attached - protects the apply page from showing a window
                // with nothing to apply for, even if one somehow got created
                // with zero courses through a different path.
                .filter(response -> !response.courses().isEmpty())
                .toList();
    }

    public void addCourseToIntake(Long intakeId, Long courseId) {
        Intake intake = findEntity(intakeId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        if (intakeCourseRepository.existsByIntakeIdAndCourseId(intakeId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This course is already open for this intake");
        }

        IntakeCourse ic = new IntakeCourse();
        ic.setIntake(intake);
        ic.setCourse(course);
        intakeCourseRepository.save(ic);
    }

    public void removeCourseFromIntake(Long intakeId, Long courseId) {
        List<IntakeCourse> all = intakeCourseRepository.findByIntakeId(intakeId);
        IntakeCourse match = all.stream()
                .filter(ic -> ic.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("This course is not attached to this intake"));

        intakeCourseRepository.delete(match);
    }

    private Intake findEntity(Long id) {
        return intakeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Intake not found"));
    }
}
