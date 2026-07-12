package com.owuor.educue.academics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerAssignmentFilterRequest {

    private String search;

    private Long trainerId;

    private Long courseId;

    private Long curriculumId;

    private Long semesterId;

    private Boolean activeOnly;
}
