package com.owuor.educue.results.controller;

import com.owuor.educue.results.dto.MarksEntryRowResponse;
import com.owuor.educue.results.dto.LegacyUnitGroupResponse;
import com.owuor.educue.results.dto.SaveMarksRequest;
import com.owuor.educue.results.service.RegistrarLegacyMarksService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registrar/legacy-results")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
public class RegistrarLegacyResultsController {

    private final RegistrarLegacyMarksService registrarLegacyMarksService;

    @GetMapping("/units")
    public List<LegacyUnitGroupResponse> getLegacyUnits() {
        return registrarLegacyMarksService.getLegacyUnits();
    }

    @GetMapping("/unit/{unitUuid}")
    public List<MarksEntryRowResponse> getResultSheet(@PathVariable UUID unitUuid) {
        return registrarLegacyMarksService.getResultSheet(unitUuid);
    }

    @PostMapping
    public MarksEntryRowResponse saveMarks(
            @RequestBody SaveMarksRequest request,
            @AuthenticationPrincipal User user) {
        return registrarLegacyMarksService.saveMarks(request, user);
    }
}
