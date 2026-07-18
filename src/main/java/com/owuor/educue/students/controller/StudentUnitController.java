package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.RegisterUnitsRequest;
import com.owuor.educue.students.dto.RegistrationResponse;
import com.owuor.educue.students.dto.StudentUnitResponse;
import com.owuor.educue.students.service.StudentUnitRegistrationService;
import com.owuor.educue.students.service.StudentUnitService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.owuor.educue.common.report.ProfessionalPdfService;
import com.owuor.educue.students.service.ExamCardService;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentUnitController {

    private final StudentUnitService studentUnitService;
    private final StudentUnitRegistrationService studentUnitRegistrationService;
    private final ProfessionalPdfService pdfService;
    private final ExamCardService examCardService;

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/units")
    public List<StudentUnitResponse> getMyCurrentPeriodUnits(
            @AuthenticationPrincipal User currentUser
    ) {

        return studentUnitService.getCurrentPeriodUnits(
                currentUser.getId()
        );
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/me/unit-registrations")
    public ResponseEntity<RegistrationResponse> registerUnits(
            @AuthenticationPrincipal User currentUser,
            @RequestBody RegisterUnitsRequest request
    ) {
        RegistrationResponse registrationResponse=  studentUnitRegistrationService.registerUnits(
                currentUser.getId(),
                request.getCourseUnitPlacementIds()
        );
        return ResponseEntity.ok(registrationResponse);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/registered-units")
    public List<StudentUnitResponse> getRegisteredUnits(
            @AuthenticationPrincipal User currentUser
    ) {
        return studentUnitRegistrationService.getRegisteredUnits(currentUser.getId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/units/pdf")
    public ResponseEntity<byte[]> downloadMyCurrentUnits(@AuthenticationPrincipal User currentUser) {
        var units = studentUnitService.getCurrentPeriodUnits(currentUser.getId());
        var rows = units.stream().map(unit -> java.util.List.of(unit.unitCode(), unit.unitName(),
                unit.academicPeriodName(), String.valueOf(unit.creditHours()))).toList();
        byte[] body = pdfService.tableReport("Current Academic Period Units", new java.util.LinkedHashMap<>(),
                java.util.List.of("Code", "Unit", "Academic period", "Credits"), rows);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-current-units.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(body);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/exam-card/pdf")
    public ResponseEntity<byte[]> downloadExamCard(@AuthenticationPrincipal User currentUser) {
        byte[] body = examCardService.generateForStudent(currentUser.getId());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-exam-card.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(body);
    }


}
