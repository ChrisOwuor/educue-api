package com.owuor.educue.institution.controller;

import com.owuor.educue.institution.dto.*;
import com.owuor.educue.institution.service.AcademicActivityDeadlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/academic-activity-deadlines") @RequiredArgsConstructor
public class AcademicActivityDeadlineController {
    private final AcademicActivityDeadlineService service;
    @PreAuthorize("hasAuthority('edit_academic_year') or hasRole('ADMIN')")
    @PutMapping public AcademicActivityDeadlineResponse save(@Valid @RequestBody AcademicActivityDeadlineRequest request) { return service.save(request); }
    @GetMapping public List<AcademicActivityDeadlineResponse> list(@RequestParam UUID academicYearUuid) { return service.list(academicYearUuid); }
    @GetMapping("/upcoming") public List<AcademicActivityDeadlineResponse> upcoming() { return service.upcoming(); }
    @PreAuthorize("hasAuthority('edit_academic_year') or hasRole('ADMIN')")
    @DeleteMapping("/{uuid}") public ResponseEntity<Void> delete(@PathVariable UUID uuid) { service.delete(uuid); return ResponseEntity.noContent().build(); }
}
