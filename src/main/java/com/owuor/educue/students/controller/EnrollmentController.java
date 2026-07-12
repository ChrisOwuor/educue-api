package com.owuor.educue.students.controller;

import com.owuor.educue.common.dto.ApiPageResponse;
import com.owuor.educue.students.dto.EnrollmentFilterRequest;
import com.owuor.educue.students.dto.EnrollmentResponse;
import com.owuor.educue.students.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('view_student')")
    public ApiPageResponse<EnrollmentResponse> getEnrollments(
            EnrollmentFilterRequest request
    ) {
        return enrollmentService.getEnrollments(request);
    }
}
