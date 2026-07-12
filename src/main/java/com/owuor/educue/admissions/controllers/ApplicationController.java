package com.owuor.educue.admissions.controllers;

import com.owuor.educue.admissions.dto.ApplicationDocumentResponse;
import com.owuor.educue.admissions.dto.ApplicationResponse;
import com.owuor.educue.admissions.dto.ApplicationResponseDto;
import com.owuor.educue.admissions.dto.CreateApplicationRequest;
import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.admissions.enums.DocumentType;
import com.owuor.educue.admissions.service.AdmissionApprovalService;
import com.owuor.educue.admissions.service.ApplicationService;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;




    private final AdmissionApprovalService admissionApprovalService;

    @PreAuthorize("hasAuthority('edit_student')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApplicationResponseDto> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
       Application application = admissionApprovalService.approve(
                id,
                currentUser
        );
       ApplicationResponseDto applicationResponseDto = new ApplicationResponseDto(application.getFullName(),application.getStatus().toString());
        // Return the newly updated application
        return ResponseEntity.ok(applicationResponseDto);
    }

    // PUBLIC - no auth. This is the actual endpoint PublicApplyPage's
    // form submits to. Must be in SecurityConfig's permitAll() list -
    // already added when we built /apply.
    @PostMapping
    public ApplicationResponse submit(@Valid @RequestBody CreateApplicationRequest request) {
        return applicationService.submit(request);
    }

    // PUBLIC - the applicant uploads documents right after submitting,
    // using the applicationId they just got back. Also needs permitAll().
    @PostMapping("/{id}/documents")
    public ApplicationDocumentResponse uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") DocumentType type,
            @RequestParam("file") MultipartFile file
    ) {
        return applicationService.uploadDocument(id, type, file);
    }

    // Everything below requires Registrar/Admin auth.

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping
    public List<ApplicationResponse> getAll() {
        return applicationService.getAll();
    }

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable Long id) {
        return applicationService.getById(id);
    }

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping("/by-intake/{intakeId}")
    public List<ApplicationResponse> getByIntake(@PathVariable Long intakeId) {
        return applicationService.getByIntake(intakeId);
    }



    @PreAuthorize("hasAuthority('edit_student')")
    @PutMapping("/{id}/reject")
    public ApplicationResponse reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return applicationService.reject(id, body.get("notes"));
    }
}
