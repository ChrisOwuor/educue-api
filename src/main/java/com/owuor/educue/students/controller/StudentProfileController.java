package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.StudentProfileResponse;
import com.owuor.educue.finance.dto.FeeLedgerResponse;
import com.owuor.educue.finance.dto.FeeStructureResponse;
import com.owuor.educue.students.service.StudentProfileService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public ResponseEntity<StudentProfileResponse> getMyProfile(
            @AuthenticationPrincipal User currentUser
    ) {

        return ResponseEntity.ok(studentProfileService.getMyProfile(
                currentUser.getId()
        ));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/fee-structure")
    public ResponseEntity<FeeStructureResponse> getMyFeeStructure(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(studentProfileService.getMyFeeStructure(
                currentUser.getId()
        ));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/ledger")
    public ResponseEntity<java.util.List<FeeLedgerResponse>> getMyLedger(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(studentProfileService.getMyLedger(
                currentUser.getId()
        ));
    }
}
