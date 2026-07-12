package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.SemesterUnitCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SemesterUnitResponse {

    private Long id;

    private Long semesterId;

    private Long unitId;

    private String unitCode;

    private String unitName;

    private Integer creditHours;

    private SemesterUnitCategory category;

    private Boolean mandatory;

    // Trainer
    private Long trainerAssignmentId;

    private Long trainerId;

    private String trainerName;
}
