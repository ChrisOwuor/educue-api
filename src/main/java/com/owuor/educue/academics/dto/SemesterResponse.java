package com.owuor.educue.academics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SemesterResponse {

    private Long id;

    private Integer yearNumber;

    private Integer semesterNumber;

    private String name;
}
