package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.enums.AcademicPeriodType;

import java.util.UUID;

public record CourseAcademicPeriodResponse(
        Long id,
        UUID uuid,
        UUID academicPeriodUuid,
        String code,
        String name,
        AcademicPeriodType periodType,
        Integer yearNumber,
        Integer periodNumber,
        Integer position,
        UUID nextPeriodUuid,
        boolean finalPeriod
) {
    public static CourseAcademicPeriodResponse from(CourseAcademicPeriod value) {
        var period = value.getAcademicPeriod();
        return new CourseAcademicPeriodResponse(
                value.getId(),
                value.getUuid(), period.getUuid(), period.getCode(), period.getName(),
                period.getPeriodType(), period.getYearNumber(), period.getPeriodNumber(),
                value.getPosition(), value.getNextPeriod() == null ? null : value.getNextPeriod().getUuid(),
                value.isFinalPeriod()
        );
    }
}
