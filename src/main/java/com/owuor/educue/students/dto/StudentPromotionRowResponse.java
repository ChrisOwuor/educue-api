package com.owuor.educue.students.dto;

import com.owuor.educue.academics.entity.Semester;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class StudentPromotionRowResponse {

    private Long enrollmentId;

    private String studentName;

    private String admissionNumber;

    private String semester;

    private int mandatoryUnits;

    private int registeredUnits;

    private int passedUnits;

    private boolean eligible;

    private String status;

    private String currentSemester;

    private int passedMandatoryUnits;
}
