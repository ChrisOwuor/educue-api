package com.owuor.educue.admissions.dto;

import com.owuor.educue.academics.entity.Course;

public record CourseSummary(Long intakeCourseId, Long courseId, java.util.UUID courseUuid, String code, String name) {
    public static CourseSummary from(com.owuor.educue.admissions.entity.IntakeCourse intakeCourse) {
        Course course = intakeCourse.getCourse();
        return new CourseSummary(intakeCourse.getId(), course.getId(), course.getUuid(), course.getCode(), course.getName());
    }
}
