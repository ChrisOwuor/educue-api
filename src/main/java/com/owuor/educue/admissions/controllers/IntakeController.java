package com.owuor.educue.admissions.controllers;

import com.owuor.educue.admissions.dto.CreateIntakeRequest;
import com.owuor.educue.admissions.dto.IntakeResponse;
import com.owuor.educue.admissions.service.IntakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/intakes")
@RequiredArgsConstructor
public class IntakeController {

    private final IntakeService intakeService;

    @PreAuthorize("hasAuthority('manage_intakes')")
    @PostMapping
    public IntakeResponse create(@Valid @RequestBody CreateIntakeRequest request) {
        return intakeService.create(request);
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @GetMapping
    public List<IntakeResponse> getAll() {
        return intakeService.getAll();
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @GetMapping("/{id}")
    public IntakeResponse getById(@PathVariable Long id) {
        return intakeService.getById(id);
    }

    // PUBLIC - no auth required. This is what the unauthenticated /apply
    // page calls to populate "which intakes can I apply to right now,
    // and which courses are open for each." Must be added to
    // SecurityConfig's permitAll() list alongside POST /api/applications.
    @GetMapping("/open")
    public List<IntakeResponse> getOpenForApplications() {
        return intakeService.getOpenForApplications();
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @PostMapping("/{id}/courses/{courseId}")
    public void addCourse(@PathVariable Long id, @PathVariable Long courseId) {
        intakeService.addCourseToIntake(id, courseId);
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @DeleteMapping("/{id}/courses/{courseId}")
    public void removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        intakeService.removeCourseFromIntake(id, courseId);
    }
}
