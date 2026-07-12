package com.owuor.educue.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FeeStructureResponse {

    private Long id;

    private Long intakeId;
    private String intakeName;

    private Long courseId;
    private String courseName;

    private Long semesterId;
    private String semesterName;

    private BigDecimal total;

    private boolean active;

    private LocalDateTime createdAt;

    private List<FeeStructureItemResponse> items;

}
