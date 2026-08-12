package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.*;
import com.owuor.educue.academics.entity.AcademicPeriod;
import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.repository.AcademicPeriodRepository;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;

    @Transactional(readOnly = true)
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

    @Transactional
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
        course.setDurationUnit(req.getDurationUnit());
        course.setName(req.getName());
        course.setDurationValue(req.getDurationValue());
        course.setQualificationType(req.getQualificationType());
        course.setStudyMode(req.getStudyMode());
        course.setTotalCredits(req.getTotalCredits());
        course.setAwardTitle(req.getAwardTitle());
        course.setActive(req.getActive() == null || req.getActive());
        Course saved = courseRepository.save(course);

        var seenPeriods = new HashSet<UUID>();
        var seenPositions = new HashSet<Integer>();
        long finalPeriodCount = req.getAcademicPeriods().stream()
                .filter(CourseAcademicPeriodRequest::finalPeriod)
                .count();
        if (finalPeriodCount != 1) {
            throw new IllegalArgumentException("A course must have exactly one final academic period");
        }
        List<CourseAcademicPeriod> coursePeriods = req.getAcademicPeriods().stream()
                .map(item -> {
                    if (!seenPeriods.add(item.academicPeriodUuid())) {
                        throw new IllegalArgumentException("An academic period can only be added once to a course");
                    }
                    if (!seenPositions.add(item.position())) {
                        throw new IllegalArgumentException("Each course academic period must have a unique position");
                    }
                    AcademicPeriod period = academicPeriodRepository.findByUuid(item.academicPeriodUuid())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Academic period not found: " + item.academicPeriodUuid()));
                    if (!period.isActive()) {
                        throw new IllegalArgumentException("Inactive academic periods cannot be added to a course");
                    }
                    CourseAcademicPeriod coursePeriod = new CourseAcademicPeriod();
                    coursePeriod.setCourse(saved);
                    coursePeriod.setAcademicPeriod(period);
                    coursePeriod.setPosition(item.position());
                    coursePeriod.setFinalPeriod(item.finalPeriod());
                    return coursePeriod;
                })
                .sorted(java.util.Comparator.comparing(CourseAcademicPeriod::getPosition))
                .toList();

        if (!coursePeriods.get(coursePeriods.size() - 1).isFinalPeriod()) {
            throw new IllegalArgumentException("Only the last course academic period can be final");
        }

        courseAcademicPeriodRepository.saveAll(coursePeriods);
        for (int i = 0; i < coursePeriods.size() - 1; i++) {
            coursePeriods.get(i).setNextPeriod(coursePeriods.get(i + 1));
        }
        courseAcademicPeriodRepository.saveAll(coursePeriods);

        return map(saved);
    }

    @Transactional
    public CourseResponse update(UUID uuid, CreateCourseRequest req) {
        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        List<CourseAcademicPeriod> existingPeriods = courseAcademicPeriodRepository
                .findByCourseIdOrderByPosition(course.getId());
        List<UUID> requestedPeriods = req.getAcademicPeriods().stream()
                .sorted(java.util.Comparator.comparing(CourseAcademicPeriodRequest::position))
                .map(CourseAcademicPeriodRequest::academicPeriodUuid)
                .toList();
        List<UUID> storedPeriods = existingPeriods.stream()
                .map(period -> period.getAcademicPeriod().getUuid())
                .toList();
        if (!storedPeriods.equals(requestedPeriods)) {
            throw new IllegalArgumentException(
                    "Academic periods cannot be replaced after course creation; update unit placements and fees separately");
        }

        course.setDepartment(department);
        course.setName(req.getName().trim());
        course.setDurationValue(req.getDurationValue());
        course.setDurationUnit(req.getDurationUnit());
        course.setQualificationType(req.getQualificationType());
        course.setStudyMode(req.getStudyMode());
        course.setTotalCredits(req.getTotalCredits());
        course.setAwardTitle(req.getAwardTitle());
        course.setActive(req.getActive() == null || req.getActive());
        return map(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public CourseResponse getByUuid(UUID uuid) {
        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        return map(course);
    }

    @Transactional(readOnly = true)
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
        res.setDepartmentName(c.getDepartment().getName());
        res.setDepartmentId(c.getDepartment().getId());
        res.setDurationUnit(c.getDurationUnit());
        res.setQualificationType(c.getQualificationType());
        res.setStudyMode(c.getStudyMode());
        res.setTotalCredits(c.getTotalCredits());
        res.setAwardTitle(c.getAwardTitle());
        res.setActive(c.isActive());
        res.setAcademicPeriods(courseAcademicPeriodRepository.findByCourseIdOrderByPosition(c.getId()).stream()
                .map(CourseAcademicPeriodResponse::from)
                .toList());

        return res;
    }

    public CourseDTO toDto(Course c) {
        CourseDTO dto = new CourseDTO();
        dto.setId(c.getId());
        dto.setQualificationType(c.getQualificationType());

        dto.setUuid(c.getUuid());
        dto.setDurationUnit(c.getDurationUnit());
        dto.setCode(c.getCode());
        dto.setName(c.getName());
        dto.setDepartmentName(c.getDepartment().getName());
        dto.setDurationValue(c.getDurationValue());
        dto.setActive(c.isActive());
        dto.setStudyMode(c.getStudyMode());
        dto.setTotalCredits(c.getTotalCredits());
        dto.setAwardTitle(c.getAwardTitle());

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
