package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.SemesterUnitCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSemesterUnitRequest {

    private Long semesterId;

    private Long unitId;

    private SemesterUnitCategory category;

    private Boolean mandatory;
}
