package com.owuor.educue.students.dto;

import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationOrigin;
import com.owuor.educue.academics.enums.RegistrationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentUnitRegistrationFilterRequest {

    private String search;

    private Long courseId;

    private Long courseAcademicPeriodId;

    private Long courseUnitPlacementId;

    private Long studentId;

    private Long academicYearId;

    private Long intakeId;

    private AttemptType attemptType;

    private RegistrationStatus status;

    private RegistrationOrigin registrationOrigin;

}
