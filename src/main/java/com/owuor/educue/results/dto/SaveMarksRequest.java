package com.owuor.educue.results.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SaveMarksRequest {

    private Long registrationId;

    private BigDecimal caMarks;

    private BigDecimal examMarks;

    private String remarks;
}
