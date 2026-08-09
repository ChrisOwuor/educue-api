package com.owuor.educue.results.controller;


import com.owuor.educue.results.dto.MarksEntryRowResponse;
import com.owuor.educue.results.dto.SaveMarksRequest;
import com.owuor.educue.results.service.LecturerMarksService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trainer/results")
@RequiredArgsConstructor
public class LecturerSubmitResultsController {

    private final LecturerMarksService lecturerMarksService;

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping("/{courseUnitPlacementId}")
    public List<MarksEntryRowResponse> getResultSheet(
            @PathVariable Long courseUnitPlacementId
    ) {
        return lecturerMarksService.getResultSheet(courseUnitPlacementId);
    }

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping("/grouped-unit/{unitUuid}")
    public List<MarksEntryRowResponse> getGroupedResultSheet(
            @PathVariable UUID unitUuid,
            @AuthenticationPrincipal User user
    ) {
        return lecturerMarksService.getGroupedResultSheet(user.getId(), unitUuid);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('record_results')")
    public ResponseEntity<MarksEntryRowResponse> saveMarks(
            @RequestBody SaveMarksRequest request,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok().body(
                lecturerMarksService.saveMarks(
                        request,
                        user
                ));
    }
}
