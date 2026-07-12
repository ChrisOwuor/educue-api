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

import java.util.List;

@RestController
@RequestMapping("/api/unit-registrations")
@RequiredArgsConstructor
public class StudentUnitRegistrationController {

    private final StudentUnitRegistrationService registrationService;
    private final RegistrationPdfService registrationPdfService;

    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    @GetMapping("/semester-unit/{semesterUnitId}")
    public RegisteredStudentsResponse getRegisteredStudents(
            @PathVariable Long semesterUnitId
    ) {
        return registrationService.getRegisteredStudents(semesterUnitId);
    }

    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    @GetMapping("non-paginated")
    public List<StudentUnitRegistrationResponse> getAllRegistrations() {

        return registrationService.getAllRegistrations();
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
