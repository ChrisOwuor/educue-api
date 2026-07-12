package com.owuor.educue.academics.dto;

import lombok.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TrainerUnitResponse {

    private Long assignmentId;

    private Long semesterUnitId;

    private Long semesterId;

    private String course;

    private String curriculum;

    private String semester;

    private String unitCode;

    private String unitName;
}
