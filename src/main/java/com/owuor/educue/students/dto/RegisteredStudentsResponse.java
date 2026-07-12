package com.owuor.educue.students.dto;


import lombok.Builder;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredStudentsResponse {

    private Long semesterUnitId;

    private String unitCode;

    private String unitName;

    private Long semesterId;

    private String semesterName;

    private Integer totalRegistered;

    private List<RegisteredStudentResponse> students;
}
