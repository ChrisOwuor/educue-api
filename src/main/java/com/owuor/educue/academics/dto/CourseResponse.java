package com.owuor.educue.academics.dto;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.academics.enums.StudyMode;
import lombok.Data;

import java.util.UUID;
import java.util.List;

@Data
public class CourseResponse {
    private Long id;
    private UUID uuid;
    private String code;
    private String name;
    private String departmentName;
    private Long departmentId;
    private Integer durationValue;
    private QualificationType qualificationType;
    private StudyMode studyMode;
    private Integer totalCredits;
    private String awardTitle;
    private String durationUnit;
    private boolean active;
    private List<CourseAcademicPeriodResponse> academicPeriods;
}
