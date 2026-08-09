package com.owuor.educue.graduation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectGraduationApplicationRequest(

        @NotBlank
        @Size(max = 500)
        String reason

) {
}
