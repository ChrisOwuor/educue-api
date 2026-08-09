package com.owuor.educue.results.controller;

import com.owuor.educue.results.dto.*;
import com.owuor.educue.results.service.AcademicTranscriptService;
import com.owuor.educue.results.service.StudentResultService;
import com.owuor.educue.results.service.StudentResultAttemptService;
import com.owuor.educue.common.report.ProfessionalPdfService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class StudentResultController {

    private final StudentResultService resultService;
    private final StudentResultAttemptService attemptService;
    private final ProfessionalPdfService pdfService;
    private final AcademicTranscriptService transcriptService;

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping
    public Page<StudentResultResponse> getResults(
            StudentResultFilterRequest filter,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return resultService.getResults(filter, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('view_own_results')")
    public List<StudentResultResponse> getMyResults(@AuthenticationPrincipal User user,
                                                    @RequestParam(required = false) Long courseAcademicPeriodId,
                                                    @RequestParam(required = false) String outcome
    ) {
        return resultService.getMyResults(user.getId(), courseAcademicPeriodId, outcome);
    }

    @GetMapping("/me/current-period")
    @PreAuthorize("hasAuthority('view_own_results')")
    public MyPeriodResultsResponse getMyCurrentPeriodResults(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            String outcome
    ) {
        return resultService.getMyCurrentPeriodResults(
                user.getId(),
                outcome
        );
    }

    @GetMapping({
            "/me/academic-results",
    })
    @PreAuthorize("hasAuthority('view_own_results')")
    public MyAcademicResultsResponse getMyAcademicResults(
            @AuthenticationPrincipal User user,

            @RequestParam(required = false)
            Long courseAcademicPeriodId,

            @RequestParam(required = false)
            String outcome
    ) {
        return resultService.getMyAcademicResults(
                user.getId(),
                courseAcademicPeriodId,
                outcome
        );
    }


    @GetMapping("/me/pdf1")
    @PreAuthorize("hasAuthority('view_own_results')")
    public ResponseEntity<byte[]> downloadMyResults1(@AuthenticationPrincipal User user, @RequestParam(required = false) Long courseAcademicPeriodId, @RequestParam(required = false) String outcome) {
        var results = resultService.getMyResults(user.getId(), courseAcademicPeriodId, outcome);
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("Student", user.getFullName());
        details.put("Results", String.valueOf(results.size()));
        var rows = results.stream().map(r -> java.util.List.of(r.unitCode(), r.unitName(), r.academicPeriod(), r.attemptType(), r.totalMarks() == null ? "-" : r.totalMarks().toPlainString(), r.grade() == null ? "-" : r.grade(), r.passed() ? "PASSED" : "FAILED")).toList();
        byte[] pdf = pdfService.tableReport("Academic Results", details, java.util.List.of("Unit code", "Unit", "Academic period", "Attempt", "Total", "Grade", "Outcome"), rows);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-academic-results.pdf").contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @PostMapping("/me/{resultId}/another-attempt")
    @PreAuthorize("hasAuthority('view_own_results')")
    public RegisterAnotherAttemptResponse anotherAttempt(@PathVariable Long resultId, @Valid @RequestBody RegisterAnotherAttemptRequest request, @AuthenticationPrincipal User user) {
        return attemptService.register(resultId, request.attemptType(), user.getId());
    }

    @GetMapping("/me/pdf")
    @PreAuthorize("hasAuthority('view_own_results')")
    public ResponseEntity<byte[]> downloadMyResults(
            @AuthenticationPrincipal User user,

            @RequestParam(required = false)
            Long courseAcademicPeriodId,

            @RequestParam(required = false)
            String outcome
    ) {
        byte[] pdf =
                transcriptService
                        .generateProvisionalTranscript(
                                user.getId(),
                                courseAcademicPeriodId,
                                outcome
                        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"provisional-academic-transcript.pdf\""
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(pdf.length)
                .body(pdf);
    }
}
