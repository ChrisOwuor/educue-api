package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.StudentProfileResponse;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.dto.FeeStructureResponse;
import com.owuor.educue.students.service.StudentProfileService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
    @GetMapping("/me/fee-structure")
    public ResponseEntity<FeeStructureResponse> getMyFeeStructure(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(studentProfileService.getMyFeeStructure(
                currentUser.getId()
        ));
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
        FeeStructureResponse structure = studentProfileService.getMyFeeStructure(currentUser.getId());
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Course", structure.getCourseName());
        details.put("Intake", structure.getIntakeName());
        details.put("Academic period", structure.getAcademicPeriodName());
        details.put("Total", "KES " + structure.getTotal());
        var rows = structure.getItems().stream().map(item -> java.util.List.of(item.getName(), "KES " + item.getAmount())).toList();
        return pdf("my-fee-structure.pdf", pdfService.tableReport("Fee Structure", details,
                java.util.List.of("Fee item", "Amount"), rows));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/fee-structures/pdf")
    public ResponseEntity<byte[]> downloadAllMyFeeStructures(@AuthenticationPrincipal User currentUser) {
        var structures = studentProfileService.getAllMyFeeStructures(currentUser.getId());
        var rows = new java.util.ArrayList<java.util.List<String>>();
        for (var structure : structures) for (var item : structure.getItems())
            rows.add(java.util.List.of(structure.getAcademicPeriodName(), item.getName(), "KES " + item.getAmount()));
        var details = new java.util.LinkedHashMap<String, String>();
        if (!structures.isEmpty()) details.put("Course", structures.getFirst().getCourseName());
        details.put("Fee structures", String.valueOf(structures.size()));
        return pdf("all-fee-structures.pdf", pdfService.tableReport("Programme Fee Structures", details,
                java.util.List.of("Academic period", "Fee item", "Amount"), rows));
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
                java.util.List.of("Posting Date", "Document No.", "Description", "Debit Amount", "Credit Amount", "Balance"), rows));
    }

    private ResponseEntity<byte[]> pdf(String filename, byte[] body) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF).body(body);
    }
}
