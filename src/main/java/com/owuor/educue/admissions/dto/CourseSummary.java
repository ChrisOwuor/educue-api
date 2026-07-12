package com.owuor.educue.admissions.dto;

import com.owuor.educue.academics.entity.Course;

public record CourseSummary(Long id, String code, String name) {
    public static CourseSummary from(Course course) {
        return new CourseSummary(course.getId(), course.getCode(), course.getName());
    }
}
