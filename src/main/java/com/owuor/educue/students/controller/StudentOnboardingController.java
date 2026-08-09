package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.*;
import com.owuor.educue.students.service.BulkOnboardingJobService;
import com.owuor.educue.students.service.StudentOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentOnboardingController {

    private final StudentOnboardingService service;
    private final BulkOnboardingJobService bulkJobService;

    /** Single student create — synchronous, returns created student details. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('create_student')")
    public CreatedStudentEnrollmentResponse create(@Valid @RequestBody CreateStudentEnrollmentRequest request) {
        return service.create(request);
    }

    /**
     * Bulk CSV upload — accepts multipart file, enqueues one job per row, returns immediately.
     * <p>
     * Expected CSV columns (comma-separated, first row = header):
     * admissionNumber, fullName, email, phone, nationalId, dateOfBirth,
     * guardianName, guardianPhone, courseId, intakeId, enrolledAcademicYearId,
     * currentAcademicYearId, currentCourseAcademicPeriodId, admissionDate,
     * migrated, openingDebit, openingCredit, openingBalanceDate, legacyReference
     */
    @PostMapping(value = "/bulk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('create_student')")
    public BulkOnboardingBatchResponse uploadBulk(@RequestParam("file") MultipartFile file) {
        return bulkJobService.enqueueCsv(file);
    }

    /** Poll the status of a previously uploaded bulk batch. */
    @GetMapping("/bulk/jobs/{batchId}")
    @PreAuthorize("hasAuthority('create_student')")
    public BulkOnboardingBatchStatus getBatchStatus(@PathVariable UUID batchId) {
        return bulkJobService.getBatchStatus(batchId);
    }
}
