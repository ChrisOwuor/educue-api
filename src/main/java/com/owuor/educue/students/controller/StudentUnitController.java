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

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentUnitController {

    private final StudentUnitService studentUnitService;
    private final StudentUnitRegistrationService studentUnitRegistrationService;

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/units")
    public List<StudentUnitResponse> getMyCurrentSemesterUnits(
            @AuthenticationPrincipal User currentUser
    ) {

        return studentUnitService.getCurrentSemesterUnits(
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
                request.getSemesterUnitIds()
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


}
