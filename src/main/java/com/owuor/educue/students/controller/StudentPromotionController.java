package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.*;
import com.owuor.educue.students.service.PromotionJobService;
import com.owuor.educue.students.service.StudentPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/academic/promotions")
@RequiredArgsConstructor
public class StudentPromotionController {

    private final StudentPromotionService promotionService;
    private final PromotionJobService promotionJobService;

    /** Paginated table of promotion candidates with eligibility status. */
    @GetMapping
    @PreAuthorize("hasAuthority('promote_students')")
    public ResponseEntity<Page<StudentPromotionRowResponse>> getPromotionDashboard(
            @RequestParam(required = false) String search, Pageable pageable) {
        return ResponseEntity.ok(promotionService.getPromotionManagementTable(search, pageable));
    }

    /**
     * Enqueue a promotion batch. Returns immediately with a batchId.
     * The PromotionWorker processes jobs in the background.
     */
    @PostMapping("/promote")
    @PreAuthorize("hasAuthority('manage_results')")
    public ResponseEntity<PromotionBatchResponse> promoteStudents(@RequestBody PromoteStudentsRequest request) {
        return ResponseEntity.accepted().body(promotionService.enqueuePromotion(request));
    }

    /** Poll batch status: pending / processing / completed / skipped / failed counts. */
    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasAuthority('promote_students')")
    public ResponseEntity<PromotionBatchStatus> getBatchStatus(@PathVariable UUID batchId) {
        return ResponseEntity.ok(promotionJobService.getBatchStatus(batchId));
    }
}
