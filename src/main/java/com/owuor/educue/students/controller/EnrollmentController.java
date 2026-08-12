package com.owuor.educue.students.controller;

import com.owuor.educue.common.dto.ApiPageResponse;
import com.owuor.educue.students.dto.EnrollmentFilterRequest;
import com.owuor.educue.students.dto.EnrollmentResponse;
import com.owuor.educue.students.dto.EnrollmentOverviewResponse;
import com.owuor.educue.students.dto.EnrollmentDetailResponse;
import com.owuor.educue.students.service.EnrollmentService;
import com.owuor.educue.graduation.dto.GraduationListDtos.Candidate;
import com.owuor.educue.graduation.dto.GraduationListDtos.GraduationCandidateDto;
import com.owuor.educue.graduation.service.GraduationListService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final GraduationListService graduationListService;

    @GetMapping
    @PreAuthorize("hasAuthority('view_student')")
    public ApiPageResponse<EnrollmentOverviewResponse> getEnrollments(
            EnrollmentFilterRequest request
    ) {
        return enrollmentService.getEnrollmentOverview(request);
    }

    /** Detailed search retained for finance workflows that need student identity. */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('view_student')")
    public ApiPageResponse<EnrollmentResponse> searchEnrollments(EnrollmentFilterRequest request) {
        return enrollmentService.getEnrollments(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('view_student')")
    public EnrollmentDetailResponse enrollment(@PathVariable UUID id) {
        return enrollmentService.getEnrollment(id);
    }

    @PostMapping("/{id}/defer")
    @PreAuthorize("hasRole('REGISTRAR')")
    public EnrollmentDetailResponse defer(@PathVariable UUID id) {
        return enrollmentService.defer(id);
    }

    @GetMapping("/hod")
    @PreAuthorize("hasRole('HOD')")
    public Page<Candidate> hodEnrollments(@AuthenticationPrincipal User user,
                                          @RequestParam(required = false) String search,
                                          @RequestParam(required = false) UUID academicPeriodUuid,
                                          @RequestParam(defaultValue = "false") boolean finalistsOnly,
                                          Pageable pageable) {
        return graduationListService.hodEnrollments(user, search, academicPeriodUuid, finalistsOnly, pageable);
    }

    @GetMapping("/hod/{id}")
    @PreAuthorize("hasRole('HOD')")
    public GraduationCandidateDto hodEnrollment(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return graduationListService.enrollmentDetail(user, id);
    }
}
