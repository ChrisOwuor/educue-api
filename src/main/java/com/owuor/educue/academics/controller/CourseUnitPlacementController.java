package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.CourseUnitPlacementResponse;
import com.owuor.educue.academics.dto.DistributeCourseUnitsRequest;
import com.owuor.educue.academics.dto.UpdateCourseUnitPlacementRequest;
import com.owuor.educue.academics.service.CourseUnitPlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.owuor.educue.common.report.ProfessionalPdfService;
import com.owuor.educue.students.service.CourseClassListPdfService;

import java.util.List;
import java.util.UUID;

/** API used by the course "Distribute units" screen and placement editor. */
@RestController
@RequiredArgsConstructor
public class CourseUnitPlacementController {

    private final CourseUnitPlacementService service;
    private final ProfessionalPdfService pdfService;
    private final CourseClassListPdfService classListPdfService;

    /** Atomically distributes one or more units into the selected course. */
    @PostMapping("/api/courses/{courseUuid}/unit-placements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('manage_courses')")
    public List<CourseUnitPlacementResponse> distribute(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody DistributeCourseUnitsRequest request
    ) {
        return service.distribute(courseUuid, request);
    }

    /** Lists the course structure, optionally as applicable to one intake year. */
    @GetMapping("/api/courses/{courseUuid}/unit-placements")
    public List<CourseUnitPlacementResponse> getForCourse(
            @PathVariable UUID courseUuid,
            @RequestParam(required = false) Integer intakeYear,
            @RequestParam(required = false) Boolean active
    ) {
        return service.getForCourse(courseUuid, intakeYear, active);
    }

    @GetMapping("/api/courses/{courseUuid}/unit-placements/pdf")
    public ResponseEntity<byte[]> downloadCourseStructure(
            @PathVariable UUID courseUuid,
            @RequestParam(required = false) UUID courseAcademicPeriodUuid
    ) {
        var placements = service.getForCourse(courseUuid, null, true).stream()
                .filter(item -> courseAcademicPeriodUuid == null
                        || courseAcademicPeriodUuid.equals(item.courseAcademicPeriodUuid()))
                .toList();
        var details = new java.util.LinkedHashMap<String, String>();
        if (!placements.isEmpty()) details.put("Course", placements.getFirst().courseName());
        if (courseAcademicPeriodUuid != null && !placements.isEmpty()) {
            details.put("Academic period", placements.getFirst().academicPeriodName());
        }
        details.put("Total units", String.valueOf(placements.size()));
        var rows = placements.stream().map(item -> java.util.List.of(
                item.academicPeriodCode(), item.unitCode(), item.unitName(),
                String.valueOf(item.creditHours()), item.effectiveFromIntakeYear() + " - " +
                        (item.effectiveToIntakeYear() == null ? "Onwards" : item.effectiveToIntakeYear()))).toList();
        byte[] pdf = pdfService.tableReport("Course Unit Structure", details,
                java.util.List.of("Academic period", "Code", "Unit", "Credits", "Effective intakes"), rows);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=course-unit-structure.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    /** Downloads active enrollments for the course at the selected progression period. */
    @GetMapping("/api/courses/{courseUuid}/academic-periods/{courseAcademicPeriodUuid}/class-list/pdf")
    @PreAuthorize("hasAnyRole('HOD', 'ADMIN', 'REGISTRAR')")
    public ResponseEntity<byte[]> downloadClassList(
            @PathVariable UUID courseUuid,
            @PathVariable UUID courseAcademicPeriodUuid
    ) {
        byte[] pdf = classListPdfService.generate(courseUuid, courseAcademicPeriodUuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=class-list.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Returns one placement for an edit screen. */
    @GetMapping("/api/course-unit-placements/{uuid}")
    public CourseUnitPlacementResponse getByUuid(@PathVariable UUID uuid) {
        return service.getByUuid(uuid);
    }

    /** Updates the period, category, effective range or active status. */
    @PutMapping("/api/course-unit-placements/{uuid}")
    @PreAuthorize("hasAuthority('manage_courses')")
    public CourseUnitPlacementResponse update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateCourseUnitPlacementRequest request
    ) {
        return service.update(uuid, request);
    }

    /** Deletes a placement that has no lecturer assignments. */
    @DeleteMapping("/api/course-unit-placements/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('manage_courses')")
    public void delete(@PathVariable UUID uuid) {
        service.delete(uuid);
    }
}
