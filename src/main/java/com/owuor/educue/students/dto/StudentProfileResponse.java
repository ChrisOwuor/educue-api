package com.owuor.educue.students.dto;

import lombok.Builder;

import java.time.LocalDate;


@Builder
public record StudentProfileResponse(

        Long studentId,

        String admissionNumber,

        String fullName,

        String email,

        String phone,

        String courseCode,

        String courseName,

        String curriculumName,

        Integer currentYear,

        Integer currentSemester,

        String currentSemesterName,

        String enrollmentStatus,

        LocalDate admissionDate

) {
}
