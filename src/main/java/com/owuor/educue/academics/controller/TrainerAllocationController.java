package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.*;
import com.owuor.educue.academics.service.TrainerAllocationService;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-allocations")
@RequiredArgsConstructor
public class TrainerAllocationController {

    private final TrainerAllocationService trainerAllocationService;

    @GetMapping
    public Page<TrainerAllocationResponse> get(
            TrainerAllocationFilterRequest filter,
            Pageable pageable
    ) {
        return trainerAllocationService.get(filter, pageable);
    }

    @GetMapping("/trainer/{trainerId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TrainerAssignmentResponse> getTrainerAssignments(
            @PathVariable Long trainerId
    ) {
        return trainerAllocationService.getTrainerAssignments(trainerId);
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public List<TrainerAssignmentResponse> getMyAssignments(
            @AuthenticationPrincipal User user
    ) {
        return trainerAllocationService.getMyAssignments(user);
    }

    @PostMapping
    public ResponseEntity<Void> assignTrainer(
            @Valid @RequestBody TrainerAssignmentRequest request,@AuthenticationPrincipal User user
    ) throws ChangeSetPersister.NotFoundException {

        trainerAllocationService.assignTrainer(request,user);

        return ResponseEntity.ok().build();
    }

}
