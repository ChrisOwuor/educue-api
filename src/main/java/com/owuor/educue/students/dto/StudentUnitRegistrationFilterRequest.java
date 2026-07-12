package com.owuor.educue.students.dto;

import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentUnitRegistrationFilterRequest {

    private String search;

    private Long courseId;

    private Long curriculumId;

    private Long semesterId;

    private Long semesterUnitId;

    private AttemptType attemptType;

    private RegistrationStatus status;

}
