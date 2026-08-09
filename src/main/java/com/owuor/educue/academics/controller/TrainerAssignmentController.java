package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.BulkTrainerAssignmentRequest;
import com.owuor.educue.academics.dto.BulkTrainerAssignmentResponse;
import com.owuor.educue.academics.dto.CourseUnitAllocationResponse;
import com.owuor.educue.academics.dto.CreateTrainerAssignmentRequest;
import com.owuor.educue.academics.dto.TrainerAssignmentResponse;
import com.owuor.educue.academics.dto.TrainerUnitGroupResponse;
import com.owuor.educue.academics.dto.TrainerUnitGroupResponse;
import com.owuor.educue.academics.service.TrainerAssignmentService;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-assignments")
@RequiredArgsConstructor
public class TrainerAssignmentController {

    private final TrainerAssignmentService trainerAssignmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('assign_trainer')")
    public TrainerAssignmentResponse create(
            @Valid @RequestBody CreateTrainerAssignmentRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return trainerAssignmentService.create(
                request,
                authenticatedUser
        );
    }

    /**
     * Returns every active course-unit placement.
     *
     * Lecturer fields are populated when a lecturer assignment
     * is active in the current academic year. Otherwise they are null.
     */
    @GetMapping("/allocation-view")
    @PreAuthorize("hasAuthority('assign_trainer')")
    public List<CourseUnitAllocationResponse> allocationView(
            @RequestParam(required = false) String search
    ) {
        return trainerAssignmentService.getAllocationView(search);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('assign_trainer')")
    public BulkTrainerAssignmentResponse createBulk(
            @Valid @RequestBody BulkTrainerAssignmentRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return trainerAssignmentService.createBulk(
                request,
                authenticatedUser
        );
    }

    @GetMapping("/me")
    public List<TrainerAssignmentResponse> getMine(
            @AuthenticationPrincipal User authenticatedUser,
            @RequestParam(required = false) Long intakeId
    ) {
        return trainerAssignmentService.getMine(
                authenticatedUser.getId(),
                intakeId
        );
    }

    @GetMapping("/me/grouped-units")
    public List<TrainerUnitGroupResponse> getMyGroupedUnits(
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return trainerAssignmentService.getMyGroupedUnits(authenticatedUser.getId());
    }

}
