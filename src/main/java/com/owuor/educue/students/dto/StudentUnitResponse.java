package com.owuor.educue.students.dto;

import com.owuor.educue.academics.enums.SemesterUnitCategory;
import lombok.Builder;

@Builder
public record StudentUnitResponse(
        Long semesterUnitId ,
        Long semesterId,
        Long unitId,
        String unitCode,
        String unitName,
        Integer creditHours,
        Boolean isMandatory,
        SemesterUnitCategory category,
        String semesterName

) {
}
