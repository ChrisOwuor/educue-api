package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.CreateCourseCurriculumRequest;
import com.owuor.educue.academics.dto.CourseCurriculumResponse;
import com.owuor.educue.academics.dto.CreateCurriculumWithStructureRequest;
import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.service.CourseCurriculumService;
import com.owuor.educue.academics.service.CurriculumPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
public class CourseCurriculumController {

    private final CourseCurriculumService curriculumService;
    private final CurriculumPdfService curriculumPdfService;

    @PostMapping
    public CourseCurriculumResponse create(
            @RequestBody CreateCourseCurriculumRequest request
    ) {
        return curriculumService.create(request);
    }

    @PostMapping("/structure")
    public ResponseEntity<CourseCurriculum> create(@RequestBody CreateCurriculumWithStructureRequest request) {
        return ResponseEntity.ok(curriculumService.createWithSemesters(request));
    }


    @GetMapping("/{id}")
    public CourseCurriculumResponse getById(
            @PathVariable Long id
    ) {
        return curriculumService.getById(id);
    }

    @GetMapping("/course/{courseId}")
    public List<CourseCurriculumResponse> getByCourse(
            @PathVariable Long courseId
    ) {
        return curriculumService.getByCourse(courseId);
    }

    @GetMapping("/{curriculumId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long curriculumId) {

        byte[] pdf = curriculumPdfService.generateCurriculumPdf(curriculumId);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=curriculum.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
