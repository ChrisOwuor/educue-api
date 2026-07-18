package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.CourseUnitPlacementItemRequest;
import com.owuor.educue.academics.dto.CourseUnitPlacementResponse;
import com.owuor.educue.academics.dto.DistributeCourseUnitsRequest;
import com.owuor.educue.academics.dto.UpdateCourseUnitPlacementRequest;
import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.entity.Unit;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.academics.repository.UnitRepository;
import com.owuor.educue.common.exception.ApiException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Handles bulk distribution and maintenance of units within course periods. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseUnitPlacementService {

    private final CourseUnitPlacementRepository placementRepository;
    private final CourseRepository courseRepository;
    private final UnitRepository unitRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;

    /** Distributes all requested units atomically: one invalid row rolls back the whole request. */
    @Transactional
    public List<CourseUnitPlacementResponse> distribute(
            UUID courseUuid,
            DistributeCourseUnitsRequest request
    ) {
        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        if (!course.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Units cannot be distributed to an inactive course");
        }

        Set<String> requestKeys = new HashSet<>();
        List<CourseUnitPlacement> placements = request.placements().stream()
                .map(item -> buildPlacement(course, item, requestKeys))
                .toList();
        return placementRepository.saveAll(placements).stream()
                .map(CourseUnitPlacementResponse::from)
                .toList();
    }

    public List<CourseUnitPlacementResponse> getForCourse(
            UUID courseUuid,
            Integer intakeYear,
            Boolean active
    ) {
        if (!courseRepository.findByUuid(courseUuid).isPresent()) {
            throw new EntityNotFoundException("Course not found");
        }
        if (intakeYear != null && intakeYear <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "intakeYear must be positive");
        }
        return placementRepository.findForCourse(courseUuid, intakeYear, active).stream()
                .map(CourseUnitPlacementResponse::from)
                .toList();
    }

    public CourseUnitPlacementResponse getByUuid(UUID uuid) {
        return CourseUnitPlacementResponse.from(findEntity(uuid));
    }

    @Transactional
    public CourseUnitPlacementResponse update(UUID uuid, UpdateCourseUnitPlacementRequest request) {
        CourseUnitPlacement placement = findEntity(uuid);
        if (request.courseAcademicPeriodUuid() != null) {
            placement.setCourseAcademicPeriod(findCoursePeriod(
                    request.courseAcademicPeriodUuid(),
                    placement.getCourseAcademicPeriod().getCourse().getId()
            ));
        }
        if (request.unitType() != null) placement.setUnitType(request.unitType());

        Integer fromYear = request.effectiveFromIntakeYear() == null
                ? placement.getEffectiveFromIntakeYear()
                : request.effectiveFromIntakeYear();
        Integer toYear = Boolean.TRUE.equals(request.clearEffectiveToIntakeYear())
                ? null
                : request.effectiveToIntakeYear() == null
                    ? placement.getEffectiveToIntakeYear()
                    : request.effectiveToIntakeYear();
        validateYearRange(fromYear, toYear);

        if (!fromYear.equals(placement.getEffectiveFromIntakeYear())
                && placementRepository.existsByCourseAcademicPeriodCourseIdAndUnitIdAndEffectiveFromIntakeYear(
                        placement.getCourseAcademicPeriod().getCourse().getId(), placement.getUnit().getId(), fromYear)) {
            throw duplicatePlacement();
        }
        placement.setEffectiveFromIntakeYear(fromYear);
        placement.setEffectiveToIntakeYear(toYear);
        if (request.active() != null) placement.setActive(request.active());
        return CourseUnitPlacementResponse.from(placementRepository.save(placement));
    }

    @Transactional
    public void delete(UUID uuid) {
        CourseUnitPlacement placement = findEntity(uuid);
        try {
            placementRepository.delete(placement);
            placementRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Placement cannot be deleted because it has lecturer assignments"
            );
        }
    }

    private CourseUnitPlacement buildPlacement(
            Course course,
            CourseUnitPlacementItemRequest item,
            Set<String> requestKeys
    ) {
        Unit unit = unitRepository.findByUuid(item.unitUuid())
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + item.unitUuid()));
        if (!unit.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Inactive unit cannot be distributed: " + unit.getCode());
        }
        CourseAcademicPeriod coursePeriod = findCoursePeriod(item.courseAcademicPeriodUuid(), course.getId());
        validateYearRange(item.effectiveFromIntakeYear(), item.effectiveToIntakeYear());

        String requestKey = item.unitUuid() + ":" + item.effectiveFromIntakeYear();
        if (!requestKeys.add(requestKey)
                || placementRepository.existsByCourseAcademicPeriodCourseIdAndUnitIdAndEffectiveFromIntakeYear(
                        course.getId(), unit.getId(), item.effectiveFromIntakeYear())) {
            throw duplicatePlacement();
        }

        CourseUnitPlacement placement = new CourseUnitPlacement();
        placement.setUnit(unit);
        placement.setCourseAcademicPeriod(coursePeriod);
        placement.setUnitType(item.unitType());
        placement.setEffectiveFromIntakeYear(item.effectiveFromIntakeYear());
        placement.setEffectiveToIntakeYear(item.effectiveToIntakeYear());
        placement.setActive(item.active() == null || item.active());
        return placement;
    }

    private CourseAcademicPeriod findCoursePeriod(UUID uuid, Long courseId) {
        CourseAcademicPeriod coursePeriod = courseAcademicPeriodRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Course academic period not found: " + uuid));
        if (!coursePeriod.getCourse().getId().equals(courseId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Course academic period does not belong to this course");
        }
        if (!coursePeriod.getAcademicPeriod().isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "An inactive academic period cannot receive units");
        }
        return coursePeriod;
    }

    private CourseUnitPlacement findEntity(UUID uuid) {
        return placementRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Course unit placement not found"));
    }

    private void validateYearRange(Integer fromYear, Integer toYear) {
        if (toYear != null && toYear < fromYear) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "effectiveToIntakeYear must be greater than or equal to effectiveFromIntakeYear"
            );
        }
    }

    private ApiException duplicatePlacement() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "The unit already has a placement starting in that intake year for this course"
        );
    }
}
