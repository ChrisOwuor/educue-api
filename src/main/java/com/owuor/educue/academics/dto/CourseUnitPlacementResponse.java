package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.enums.AcademicPeriodType;
import com.owuor.educue.academics.enums.UnitType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Placement details needed to render and edit a course's distributed units. */
public record CourseUnitPlacementResponse(
        UUID uuid,
        UUID courseUuid,
        String courseCode,
        String courseName,
        UUID courseAcademicPeriodUuid,
        UUID unitUuid,
        String unitCode,
        String unitName,
        Integer creditHours,
        UUID academicPeriodUuid,
        String academicPeriodCode,
        String academicPeriodName,
        AcademicPeriodType academicPeriodType,
        Integer courseAcademicPeriodPosition,
        UnitType unitType,
        Integer effectiveFromIntakeYear,
        Integer effectiveToIntakeYear,
        boolean active,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CourseUnitPlacementResponse from(CourseUnitPlacement placement) {
        var coursePeriod = placement.getCourseAcademicPeriod();
        var course = coursePeriod.getCourse();
        var period = coursePeriod.getAcademicPeriod();
        return new CourseUnitPlacementResponse(
                placement.getUuid(),
                course.getUuid(), course.getCode(), course.getName(), coursePeriod.getUuid(),
                placement.getUnit().getUuid(), placement.getUnit().getCode(), placement.getUnit().getName(),
                placement.getUnit().getCreditHours(),
                period.getUuid(), period.getCode(), period.getName(), period.getPeriodType(),
                coursePeriod.getPosition(), placement.getUnitType(),
                placement.getEffectiveFromIntakeYear(), placement.getEffectiveToIntakeYear(),
                placement.isActive(), placement.getVersion(), placement.getCreatedAt(), placement.getUpdatedAt()
        );
    }
}
