package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.RegisteredStudentResponse;
import com.owuor.educue.students.dto.RegisteredStudentsResponse;
import com.owuor.educue.students.dto.StudentUnitRegistrationFilterRequest;
import com.owuor.educue.students.dto.StudentUnitRegistrationResponse;
import com.owuor.educue.students.service.RegistrationPdfService;
import com.owuor.educue.students.service.StudentUnitRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.owuor.educue.users.entity.User;

import java.util.List;
import java.util.UUID;
import com.owuor.educue.students.dto.HodUnitRegistrationDtos.*;

@RestController
@RequestMapping("/api/unit-registrations")
@RequiredArgsConstructor
public class StudentUnitRegistrationController {

    private final StudentUnitRegistrationService registrationService;
    private final RegistrationPdfService registrationPdfService;

    @PreAuthorize("hasRole('HOD')")
    @GetMapping("/hod/intakes")
    public List<IntakeOption> hodIntakes(@AuthenticationPrincipal User requester) {
        return registrationService.hodIntakes(requester);
    }

    @PreAuthorize("hasRole('HOD')")
    @GetMapping("/hod/enrollments")
    public List<EnrollmentOption> hodEnrollments(@AuthenticationPrincipal User requester, @RequestParam UUID intakeUuid,
                                                  @RequestParam(required = false) String search) {
        return registrationService.hodEnrollments(requester, intakeUuid, search);
    }

    @PreAuthorize("hasRole('HOD')")
    @PostMapping("/hod/available-units")
    public List<UnitOption> hodAvailableUnits(@AuthenticationPrincipal User requester, @RequestBody SelectionRequest request) {
        return registrationService.hodAvailableUnits(requester, request);
    }

    @PreAuthorize("hasRole('HOD')")
    @PostMapping("/hod/register")
    public RegisterResponse hodRegister(@AuthenticationPrincipal User requester, @RequestBody RegisterRequest request) {
        return registrationService.hodRegister(requester, request);
    }

    @PreAuthorize("hasAnyRole('TRAINER','HOD','ADMIN')")
    @GetMapping("/course-unit-placement/{courseUnitPlacementId}")
    public RegisteredStudentsResponse getRegisteredStudents(
            @PathVariable Long courseUnitPlacementId,
            @AuthenticationPrincipal User requester
    ) {
        return registrationService.getRegisteredStudents(courseUnitPlacementId, requester);
    }

    @PreAuthorize("hasAnyRole('TRAINER','HOD','ADMIN')")
    @GetMapping("/course-unit-placement/{courseUnitPlacementId}/exam-list")
    public ResponseEntity<byte[]> exportExamList(
            @PathVariable Long courseUnitPlacementId,
            @AuthenticationPrincipal User requester
    ) {
        registrationService.assertCanViewPlacement(courseUnitPlacementId, requester);
        byte[] pdf = registrationPdfService.generateProfessionalExamListPdf(courseUnitPlacementId, requester.getFullName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exam-list.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PreAuthorize("hasAnyRole('TRAINER','HOD','ADMIN')")
    @GetMapping("/course-unit-placement/{courseUnitPlacementId}/exam-submission-checklist")
    public ResponseEntity<byte[]> exportExamSubmissionChecklist(
            @PathVariable Long courseUnitPlacementId,
            @AuthenticationPrincipal User requester
    ) {
        registrationService.assertCanViewPlacement(courseUnitPlacementId, requester);
        byte[] pdf = registrationPdfService.generateExamSubmissionChecklistPdf(courseUnitPlacementId, requester.getFullName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exam-submission-checklist.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PreAuthorize("hasAnyRole('TRAINER','HOD','ADMIN')")
    @GetMapping("/course-unit-placement/{courseUnitPlacementId}/students-list")
    public ResponseEntity<byte[]> exportStudentsList(
            @PathVariable Long courseUnitPlacementId,
            @AuthenticationPrincipal User requester
    ) {
        registrationService.assertCanViewPlacement(courseUnitPlacementId, requester);
        StudentUnitRegistrationFilterRequest filter = new StudentUnitRegistrationFilterRequest();
        filter.setCourseUnitPlacementId(courseUnitPlacementId);
        byte[] pdf = registrationPdfService.generateRegistrationsPdf(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=registered-students.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping
    public Page<StudentUnitRegistrationResponse> getRegistrations(
            StudentUnitRegistrationFilterRequest filter,
            @PageableDefault(
                    size = 20,
                    sort = "registeredAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return registrationService.getRegistrations(filter, pageable);
    }

    // Same filter shape as the list endpoint above - whatever the HOD has
    // currently searched/filtered for is exactly what ends up in the PDF,
    // just without pagination so the full matching set is included.
    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPdf(StudentUnitRegistrationFilterRequest filter) {
        byte[] pdf = registrationPdfService.generateRegistrationsPdf(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=unit-registrations.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }


}
