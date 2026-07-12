package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.BulkPromotionRequest;
import com.owuor.educue.students.dto.PromoteStudentsRequest;
import com.owuor.educue.students.dto.PromotionResultResponse;
import com.owuor.educue.students.dto.StudentPromotionRowResponse;
import com.owuor.educue.students.service.StudentPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic/promotions")
@RequiredArgsConstructor
public class StudentPromotionController {

    private final StudentPromotionService promotionService;

//    @PostMapping
//    @PreAuthorize("hasAuthority('promote_students')")
//    public ResponseEntity<List<PromotionResultResponse>> processBulkPromotion(
//            @RequestBody BulkPromotionRequest request
//    ) {
//        return ResponseEntity.ok(promotionService.promoteStudents(request));
//    }

    @GetMapping
    @PreAuthorize("hasAuthority('promote_students')") // Matches your access control configuration
    public ResponseEntity<Page<StudentPromotionRowResponse>> getPromotionDashboard(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(promotionService.getPromotionManagementTable(search, pageable));
    }


    @PostMapping("/promote")
    @PreAuthorize("hasAuthority('manage_results')")
    public void promoteStudents(
            @RequestBody PromoteStudentsRequest request
    ) {
        promotionService.promoteStudents(request);
    }
}
