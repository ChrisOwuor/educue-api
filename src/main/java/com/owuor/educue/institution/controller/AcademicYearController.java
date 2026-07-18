package com.owuor.educue.institution.controller;

import com.owuor.educue.institution.dto.AcademicYearResponse;
import com.owuor.educue.institution.dto.CreateAcademicYearRequest;
import com.owuor.educue.institution.dto.UpdateAcademicYearRequest;
import com.owuor.educue.institution.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** REST endpoints for creating and managing institutional academic years. */
@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService service;

    /** Creates an academic year. Setting current=true replaces the existing current year. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('manage_courses')")
    public AcademicYearResponse create(@Valid @RequestBody CreateAcademicYearRequest request) {
        return service.create(request);
    }

    /** Returns all academic years, newest start date first. */
    @GetMapping
    public List<AcademicYearResponse> getAll() {
        return service.getAll();
    }

    /** Returns the academic year currently active for operations. */
    @GetMapping("/current")
    public AcademicYearResponse getCurrent() {
        return service.getCurrent();
        
    }

    /** Returns one academic year by its public UUID. */
    @GetMapping("/{uuid}")
    public AcademicYearResponse getByUuid(@PathVariable UUID uuid) {
        return service.getByUuid(uuid);
    }

    /** Partially updates an academic year; omitted JSON properties remain unchanged. */
    @PutMapping("/{uuid}")
    @PreAuthorize("hasAuthority('manage_courses')")
    public AcademicYearResponse update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateAcademicYearRequest request
    ) {
        return service.update(uuid, request);
    }

    /** Makes the selected active, open year current and clears the previous current year. */
    @PatchMapping("/{uuid}/make-current")
    @PreAuthorize("hasAuthority('manage_courses')")
    public AcademicYearResponse makeCurrent(@PathVariable UUID uuid) {
        return service.makeCurrent(uuid);
    }

    /** Deletes a non-current academic year. Referenced years remain protected by the database. */
    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('manage_courses')")
    public void delete(@PathVariable UUID uuid) {
        service.delete(uuid);
    }
}
