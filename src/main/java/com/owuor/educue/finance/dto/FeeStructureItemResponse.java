package com.owuor.educue.finance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FeeStructureItemResponse {

    private Long id;

    private String name;

    private BigDecimal amount;

}
