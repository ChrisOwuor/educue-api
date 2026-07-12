package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.SemesterResponse;
import com.owuor.educue.academics.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping("/{curriculumId}")
    public List<SemesterResponse> getByCurriculum(
            @PathVariable Long curriculumId
    ) {
        return semesterService.getByCurriculum(curriculumId);
    }
}
