package com.owuor.educue.students.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PromoteStudentsRequest {

    private List<Long> enrollmentIds;

}
