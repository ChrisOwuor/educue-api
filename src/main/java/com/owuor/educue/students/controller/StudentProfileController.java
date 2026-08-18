package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.StudentProfileResponse;
import com.owuor.educue.students.dto.CompleteStudentProfileRequest;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.service.PeriodFeeStructureService;
import com.owuor.educue.students.service.StudentProfileService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.owuor.educue.common.report.ProfessionalPdfService;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final ProfessionalPdfService pdfService;
    private final PeriodFeeStructureService periodFeeStructureService;

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public ResponseEntity<StudentProfileResponse> getMyProfile(
            @AuthenticationPrincipal User currentUser
    ) {

        return ResponseEntity.ok(studentProfileService.getMyProfile(
                currentUser.getId()
        ));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/me/profile-completion")
    public ResponseEntity<StudentProfileResponse> completeMyProfile(
            @AuthenticationPrincipal User currentUser,
            @jakarta.validation.Valid @RequestBody CompleteStudentProfileRequest request
    ) {
        return ResponseEntity.ok(studentProfileService.completeMyProfile(currentUser.getId(), request));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/ledger")
    public ResponseEntity<java.util.List<FeeLedgerResponse>> getMyLedger(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(studentProfileService.getMyLedger(
                currentUser.getId()
        ));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/fee-structure/pdf")
    public ResponseEntity<byte[]> downloadMyFeeStructure(@AuthenticationPrincipal User currentUser) {
        var structure = periodFeeStructureService.getFees(currentUser);
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Academic period", structure.academicPeriodCode() + " — " + structure.academicPeriodName());
        details.put("Intake", structure.intakeName());
        details.put("Total", "KES " + structure.total());
        var rows = structure.items().stream().map(item ->
                java.util.List.of(item.code(), item.name(), "KES " + item.amount())).toList();
        return pdf("my-fee-structure.pdf", pdfService.tableReport("Fee Schedule", details,
                java.util.List.of("Code", "Fee item", "Amount"), rows));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/fee-statement/pdf")
    public ResponseEntity<byte[]> downloadMyFeeStatement(@AuthenticationPrincipal User currentUser) {
        var profile = studentProfileService.getMyProfile(currentUser.getId());
        var ledger = studentProfileService.getMyLedger(currentUser.getId());
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Student", profile.fullName());
        details.put("Admission number", profile.admissionNumber());
        details.put("Course", profile.courseName());
        details.put("Academic period", profile.academicPeriodName());
        var rows = ledger.stream().map(item -> java.util.List.of(item.getPostingDate().toString(),
                item.getDocumentNumber(), item.getDescription(), item.getDebit().toString(),
                item.getCredit().toString(), item.getRunningBalance().toString())).toList();
        return pdf("my-fee-statement.pdf", pdfService.tableReport("Student Account Statement", details,
                java.util.List.of("Date", "Document No.", "Description", "Debit", "Credit", "Balance"), rows,
                new float[]{1.15f, 1.30f, 3.05f, 1.15f, 1.15f, 1.10f}));
    }

    private ResponseEntity<byte[]> pdf(String filename, byte[] body) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF).body(body);
    }
}
