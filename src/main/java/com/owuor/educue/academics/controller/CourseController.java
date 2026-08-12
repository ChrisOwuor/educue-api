package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.CourseDTO;
import com.owuor.educue.academics.dto.CourseFilterRequest;
import com.owuor.educue.academics.dto.CreateCourseRequest;
import com.owuor.educue.academics.dto.CourseResponse;
import com.owuor.educue.academics.service.CourseService;
import com.owuor.educue.common.dto.ApiPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiPageResponse<CourseDTO>> getCourses(CourseFilterRequest req) {
        return ResponseEntity.ok(courseService.getCourses(req));
    }

    @PostMapping
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest req) {
        return courseService.create(req);
    }

    @PutMapping("/{uuid}")
    public CourseResponse update(@PathVariable UUID uuid, @Valid @RequestBody CreateCourseRequest req) {
        return courseService.update(uuid, req);
    }


    @GetMapping("/{uuid}")
    public CourseResponse getByUuid(@PathVariable UUID uuid) {
        return courseService.getByUuid(uuid);
    }

    @GetMapping("/department/{departmentId}")
    public List<CourseResponse> getByDepartment(@PathVariable Long departmentId) {
        return courseService.getByDepartment(departmentId);
    }
}
