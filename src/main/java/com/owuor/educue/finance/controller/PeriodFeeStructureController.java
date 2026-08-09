package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.PeriodFeeStructureResponse;
import com.owuor.educue.finance.dto.SavePeriodFeeStructureRequest;
import com.owuor.educue.finance.dto.StudentFeesResponse;
import com.owuor.educue.finance.service.PeriodFeeStructureService;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/period-fees")
@RequiredArgsConstructor
public class PeriodFeeStructureController {

    private final PeriodFeeStructureService periodFeeStructureService;

    @GetMapping("/structure")
    public PeriodFeeStructureResponse getStructure(
            @RequestParam
            UUID courseAcademicPeriodUuid,

            @RequestParam
            Long intakeId
    ) {
        return periodFeeStructureService.getStructure(
                courseAcademicPeriodUuid,
                intakeId
        );
    }

    @PutMapping("/structure")
    public PeriodFeeStructureResponse saveStructure(
            @Valid
            @RequestBody
            SavePeriodFeeStructureRequest request
    ) {
        return periodFeeStructureService.saveStructure(
                request
        );
    }

    @PutMapping("/structure/confirm")
    public PeriodFeeStructureResponse saveAndConfirmStructure(
            @Valid @RequestBody SavePeriodFeeStructureRequest request
    ) {
        return periodFeeStructureService.saveAndConfirmStructure(request);
    }

    @GetMapping("/student")
    public StudentFeesResponse getMyFees(
            @AuthenticationPrincipal User user
    ) {
        return periodFeeStructureService.getFees(user);
    }
}
