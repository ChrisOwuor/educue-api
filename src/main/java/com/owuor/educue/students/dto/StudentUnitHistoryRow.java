package com.owuor.educue.students.dto;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.students.entity.StudentUnitRegistration;

public record StudentUnitHistoryRow(
        CourseUnitPlacement placement,
        StudentUnitRegistration registration
) {
}
