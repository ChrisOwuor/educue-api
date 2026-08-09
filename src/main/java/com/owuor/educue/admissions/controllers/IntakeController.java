package com.owuor.educue.admissions.controllers;

import com.owuor.educue.admissions.dto.CreateIntakeRequest;
import com.owuor.educue.admissions.dto.ConfirmationRequest;
import com.owuor.educue.admissions.dto.IntakeDetailResponse;
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

    @PreAuthorize("hasAnyAuthority('manage_intakes', 'manage_courses') or hasAnyRole('ADMIN', 'FINANCE')")
    @GetMapping
    public List<IntakeResponse> getAll() {
        return intakeService.getAll();
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @GetMapping("/{id}")
    public IntakeResponse getById(@PathVariable Long id) {
        return intakeService.getById(id);
    }

    @PreAuthorize("hasAnyAuthority('manage_intakes', 'manage_courses') or hasAnyRole('ADMIN', 'FINANCE')")
    @GetMapping("/{id}/detail")
    public IntakeDetailResponse getDetail(@PathVariable Long id) {
        return intakeService.getDetail(id);
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

    @PreAuthorize("hasAuthority('manage_courses')")
    @PutMapping("/{id}/courses/{courseId}/academic-confirmation")
    public IntakeDetailResponse confirmAcademic(@PathVariable Long id, @PathVariable Long courseId,
            @Valid @RequestBody ConfirmationRequest request) {
        return intakeService.confirmAcademic(id, courseId, request.confirmed());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @PutMapping("/{id}/courses/{courseId}/fee-confirmation")
    public IntakeDetailResponse confirmFees(@PathVariable Long id, @PathVariable Long courseId,
            @Valid @RequestBody ConfirmationRequest request) {
        return intakeService.confirmFees(id, courseId, request.confirmed());
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @PutMapping("/{id}/publish")
    public IntakeDetailResponse publish(@PathVariable Long id) {
        return intakeService.publish(id);
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @DeleteMapping("/{id}/courses/{courseId}")
    public void removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        intakeService.removeCourseFromIntake(id, courseId);
    }

    @PreAuthorize("hasAuthority('manage_intakes')")
    @PutMapping("/{id}/close")
    public IntakeResponse close(@PathVariable Long id) {
        return intakeService.close(id);
    }
}
