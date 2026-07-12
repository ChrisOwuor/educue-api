package com.owuor.educue.academics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerAllocationFilterRequest {

    private String search;

    private Long courseId;

    private Long curriculumId;

    private Long semesterId;
}
