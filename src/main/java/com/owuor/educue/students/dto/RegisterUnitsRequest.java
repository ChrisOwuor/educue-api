package com.owuor.educue.students.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUnitsRequest {

    private List<Long> courseUnitPlacementIds;

}
