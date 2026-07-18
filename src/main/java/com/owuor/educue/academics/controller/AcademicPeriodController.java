package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.AcademicPeriodResponse;
import com.owuor.educue.academics.dto.CreateAcademicPeriodRequest;
import com.owuor.educue.academics.dto.UpdateAcademicPeriodRequest;
import com.owuor.educue.academics.enums.AcademicPeriodType;
import com.owuor.educue.academics.service.AcademicPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Full CRUD API for reusable academic periods such as semesters, terms and modules. */
@RestController
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodService service;

    /** Creates a period and returns it with its generated public UUID. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('manage_courses')")
    public AcademicPeriodResponse create(@Valid @RequestBody CreateAcademicPeriodRequest request) {
        return service.create(request);
    }

    /** Lists periods in sequence order, optionally filtered by periodType and active. */
    @GetMapping
    public List<AcademicPeriodResponse> getAll(
            @RequestParam(required = false) AcademicPeriodType periodType,
            @RequestParam(required = false) Boolean active
    ) {
        return service.getAll(periodType, active);
    }

    /** Gets one academic period using its public UUID. */
    @GetMapping("/{uuid}")
    public AcademicPeriodResponse getByUuid(@PathVariable UUID uuid) {
        return service.getByUuid(uuid);
    }

    /** Updates supplied fields while leaving omitted fields unchanged. */
    @PutMapping("/{uuid}")
    @PreAuthorize("hasAuthority('manage_courses')")
    public AcademicPeriodResponse update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateAcademicPeriodRequest request
    ) {
        return service.update(uuid, request);
    }

    /** Deletes an unused period; referenced periods return HTTP 409 Conflict. */
    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('manage_courses')")
    public void delete(@PathVariable UUID uuid) {
        service.delete(uuid);
    }
}
