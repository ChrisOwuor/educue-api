package com.owuor.educue.students.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class StudentPromotionRowResponse {

    private Long enrollmentId;

    private String studentName;

    private String admissionNumber;

    private String courseName;

    private String academicPeriod;

    private int coreUnits;

    private int registeredUnits;

    private int passedUnits;

    private boolean eligible;

    private String status;

    private String currentAcademicPeriod;

    private int passedCoreUnits;
}
