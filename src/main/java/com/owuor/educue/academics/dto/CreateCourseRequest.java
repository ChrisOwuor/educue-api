package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.academics.enums.StudyMode;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Data
public class CreateCourseRequest {
    private Long departmentId;
    private String name;
    @NotNull @Positive
    private Integer durationValue;
    @NotNull
    private QualificationType qualificationType;
    @NotNull
    private StudyMode studyMode;
    @Positive
    private Integer totalCredits;
    private String durationUnit;
    private String awardTitle;
    private Boolean active;
    @NotEmpty
    private List<@Valid CourseAcademicPeriodRequest> academicPeriods;
}
