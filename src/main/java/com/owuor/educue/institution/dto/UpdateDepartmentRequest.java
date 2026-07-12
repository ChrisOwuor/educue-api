package com.owuor.educue.institution.dto;

import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @Size(max = 100) String name,
        @Size(max = 255) String description,
        // Boxed Boolean (not primitive boolean) so a request that omits this
        // field doesn't silently get deserialized as `false` and deactivate
        // the department by accident.
        Boolean active
) {}
