package com.owuor.educue.institution.controller;

import com.owuor.educue.institution.dto.CreateDepartmentRequest;
import com.owuor.educue.institution.dto.DepartmentResponse;
import com.owuor.educue.institution.dto.UpdateDepartmentRequest;
import com.owuor.educue.institution.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PreAuthorize("hasAuthority('manage_courses')")
    @PostMapping
    public DepartmentResponse create(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentService.create(request);
    }

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping("/{id}")
    public DepartmentResponse getById(@PathVariable Long id) {
        return departmentService.getById(id);
    }

    @PreAuthorize("hasAuthority('view_student')")
    @GetMapping
    public List<DepartmentResponse> getAll() {
        return departmentService.getAll();
    }

    @PreAuthorize("hasAuthority('manage_courses')")
    @PutMapping("/{id}")
    public DepartmentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        return departmentService.update(id, request);
    }

    @PreAuthorize("hasAuthority('manage_courses')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
