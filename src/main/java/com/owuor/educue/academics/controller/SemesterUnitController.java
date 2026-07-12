
package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.*;
import com.owuor.educue.academics.service.CourseService;
import com.owuor.educue.academics.service.SemesterUnitService;
import com.owuor.educue.common.dto.ApiPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;



@RestController
@RequestMapping("/api/semester-units")
@RequiredArgsConstructor
public class SemesterUnitController {

    private final SemesterUnitService semesterUnitService;

    @PostMapping
    public ResponseEntity<SemesterUnitResponse> create(
            @RequestBody CreateSemesterUnitRequest request
    ) {

        return ResponseEntity.ok(
                semesterUnitService.create(request)
        );
    }

    @GetMapping("/{semesterId}")
    public ResponseEntity<List<SemesterUnitResponse>> getBySemester(
            @PathVariable Long semesterId
    ) {

        return ResponseEntity.ok(
                semesterUnitService.getBySemester(semesterId)
        );
    }
}
