package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.CourseUnitStructureResponse;
import com.owuor.educue.academics.dto.SaveCourseUnitStructureRequest;
import com.owuor.educue.academics.service.CourseUnitPlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/courses/{courseUuid}/unit-placements"
)
@RequiredArgsConstructor
public class CourseUnitPlacementController {

    private final CourseUnitPlacementService service;

    @GetMapping("/structure")
    @PreAuthorize("hasAuthority('manage_courses')")
    public CourseUnitStructureResponse getStructure(

            @PathVariable
            UUID courseUuid,

            @RequestParam
            UUID courseAcademicPeriodUuid,

            @RequestParam
            Long intakeId
    ) {
        return service.getStructure(
                courseUuid,
                courseAcademicPeriodUuid,
                intakeId
        );
    }

    @PutMapping("/structure")
    @PreAuthorize("hasAuthority('manage_courses')")
    public CourseUnitStructureResponse saveStructure(

            @PathVariable
            UUID courseUuid,

            @Valid
            @RequestBody
            SaveCourseUnitStructureRequest request
    ) {
        return service.saveStructure(
                courseUuid,
                request
        );
    }

    @PutMapping("/structure/confirm")
    @PreAuthorize("hasAuthority('manage_courses')")
    public CourseUnitStructureResponse saveAndConfirmStructure(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody SaveCourseUnitStructureRequest request
    ) {
        return service.saveAndConfirmStructure(courseUuid, request);
    }
}
