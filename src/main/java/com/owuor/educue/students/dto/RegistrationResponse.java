package com.owuor.educue.students.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationResponse {
    private String message;
    private int registeredUnits;
}
