package com.owuor.educue.results.controller;

import com.owuor.educue.results.dto.StudentResultFilterRequest;
import com.owuor.educue.results.dto.StudentResultResponse;
import com.owuor.educue.results.service.StudentResultService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class StudentResultController {

    private final StudentResultService resultService;

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
    public List<StudentResultResponse> getMyResults(
            @AuthenticationPrincipal User user
    ) {
        return resultService.getMyResults(user.getId());
    }
}
