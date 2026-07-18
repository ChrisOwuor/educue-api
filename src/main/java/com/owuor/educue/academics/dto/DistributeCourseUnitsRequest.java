package com.owuor.educue.academics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Atomic bulk request used to distribute units into a course structure. */
public record DistributeCourseUnitsRequest(
        @NotEmpty List<@Valid CourseUnitPlacementItemRequest> placements
) {}
