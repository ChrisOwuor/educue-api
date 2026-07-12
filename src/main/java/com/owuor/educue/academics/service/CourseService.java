package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.CourseDTO;
import com.owuor.educue.academics.dto.CourseFilterRequest;
import com.owuor.educue.academics.dto.CreateCourseRequest;
import com.owuor.educue.academics.dto.CourseResponse;
import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.academics.repository.CourseSpecification;
import com.owuor.educue.common.dto.ApiPageResponse;
import com.owuor.educue.institution.entity.Department;
import com.owuor.educue.institution.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    public ApiPageResponse<CourseDTO> getCourses(CourseFilterRequest req) {

        Pageable pageable = PageRequest.of(
                req.getPage(),
                req.getSize(),
                buildSort(req.getSort())
        );

        Specification<Course> spec =
                Specification.where(CourseSpecification.search(req.getSearch()))
                        .and(CourseSpecification.status(req.getStatus()))
                        .and(CourseSpecification.department(req.getDepartmentId()));

        Page<Course> page = courseRepository.findAll(spec, pageable);

        List<CourseDTO> content = page.getContent()
                .stream()
                .map(this::toDto)
                .toList();

        return ApiPageResponse.<CourseDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private Sort buildSort(String sort) {
        if (sort == null) return Sort.by("id").descending();

        String[] parts = sort.split(",");

        return parts.length == 2 && parts[1].equalsIgnoreCase("desc")
                ? Sort.by(parts[0]).descending()
                : Sort.by(parts[0]).ascending();
    }

    public CourseResponse create(CreateCourseRequest req) {


            var code = generateCourseCode(req.getName());


        // ✅ ensure uniqueness (retry loop)
        while (courseRepository.existsByCode(code)) {
            code = generateCourseCode(req.getName());
        }

        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        Course course = new Course();
        course.setDepartment(dept);
        course.setCode(code);
        course.setName(req.getName());
        course.setDurationValue(req.getDurationValue());
        course.setDurationUnit(req.getDurationUnit());
        course.setTotalSemesters(req.getTotalSemesters());
        Course saved = courseRepository.save(course);

        return map(saved);
    }

    public CourseResponse getByUuid(UUID uuid) {
        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        return map(course);
    }

    public List<CourseResponse> getByDepartment(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId)
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    private CourseResponse map(Course c) {
        CourseResponse res = new CourseResponse();
        res.setId(c.getId());
        res.setUuid(c.getUuid());
        res.setCode(c.getCode());
        res.setName(c.getName());
        res.setDurationValue(c.getDurationValue());
        res.setDurationUnit(c.getDurationUnit());
        res.setTotalSemesters(c.getTotalSemesters());
        res.setDepartmentName(c.getDepartment().getName());
        return res;
    }

    public CourseDTO toDto(Course c) {
        CourseDTO dto = new CourseDTO();
        dto.setId(c.getId());

        dto.setUuid(c.getUuid());
        dto.setCode(c.getCode());
        dto.setName(c.getName());
        dto.setDepartmentName(c.getDepartment().getName());
        dto.setDurationValue(c.getDurationValue());
        dto.setDurationUnit(c.getDurationUnit());
        dto.setTotalSemesters(c.getTotalSemesters());
        dto.setActive(c.isActive());

        return dto;
    }

    private String generateCourseCode(String name) {
        String base = name
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(4, name.length()));

        String random = String.valueOf((int)(Math.random() * 900 + 100));

        return base + random;
    }
}
