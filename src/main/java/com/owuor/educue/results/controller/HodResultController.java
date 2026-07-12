package com.owuor.educue.results.controller;

import com.owuor.educue.results.dto.BatchApprovalRequest;
import com.owuor.educue.results.service.StudentResultService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class HodResultController {

    private final StudentResultService resultService;

    @PostMapping("/batch-status")
    @PreAuthorize("hasAuthority('approve_results')") // Enforce your security authority string
    public ResponseEntity<Void> updateBatchStatus(
            @RequestBody BatchApprovalRequest request,
            @AuthenticationPrincipal User user
    ) {
        resultService.updateBatchStatus(request, user);
        return ResponseEntity.ok().build();
    }
}
