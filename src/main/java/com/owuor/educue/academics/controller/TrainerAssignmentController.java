package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.CreateTrainerAssignmentRequest;
import com.owuor.educue.academics.dto.TrainerAssignmentFilterRequest;
import com.owuor.educue.academics.dto.TrainerAssignmentResponse;
import com.owuor.educue.academics.service.TrainerAssignmentService;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-assignments")
@RequiredArgsConstructor
public class TrainerAssignmentController {

    private final TrainerAssignmentService trainerAssignmentService;

    @PostMapping
    public TrainerAssignmentResponse create(
            @Valid @RequestBody CreateTrainerAssignmentRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return trainerAssignmentService.create(request, authenticatedUser);
    }

    @GetMapping
    public Page<TrainerAssignmentResponse> get(
            TrainerAssignmentFilterRequest filter,
            Pageable pageable
    ) {
        return trainerAssignmentService.get(filter, pageable);
    }


}
