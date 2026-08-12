package com.owuor.educue.graduation.controller;

import com.owuor.educue.graduation.dto.GraduationListDtos.*;
import com.owuor.educue.graduation.service.GraduationListService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/graduation-lists")
@RequiredArgsConstructor
public class GraduationListController {
    private final GraduationListService service;

    @PostMapping("/hod/candidates/{id}/assess")
    @PreAuthorize("hasRole('HOD')")
    public GraduationCandidateDto assess(@AuthenticationPrincipal User u, @PathVariable UUID id, @RequestBody AssessRequest r) {
        return service.assess(u, r.academicYearUuid(), id);
    }

    @DeleteMapping("/hod/candidates/{id}/assessment")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('HOD')")
    public void clearAssessment(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        service.clearAssessment(user, id);
    }

    @PostMapping("/hod/entries")
    @PreAuthorize("hasRole('HOD')")
    public GraduationCandidateDto add(@AuthenticationPrincipal User u, @RequestBody AddRequest r) {
        return service.add(u, r);
    }

    @PostMapping("/hod/lists")
    @PreAuthorize("hasRole('HOD')")
    public Summary createList(@AuthenticationPrincipal User u, @RequestBody YearRequest r) {
        return service.createList(u, r.academicYearUuid());
    }

    @PutMapping("/hod/lists/{listId}")
    @PreAuthorize("hasRole('HOD')")
    public Summary updateList(@AuthenticationPrincipal User u, @PathVariable UUID listId, @RequestBody YearRequest r) {
        return service.updateList(u, listId, r.academicYearUuid());
    }

    @DeleteMapping("/hod/lists/{listId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('HOD')")
    public void deleteList(@AuthenticationPrincipal User u, @PathVariable UUID listId) {
        service.deleteList(u, listId);
    }

    @GetMapping("/hod/lists")
    @PreAuthorize("hasRole('HOD')")
    public Page<Summary> hodLists(@AuthenticationPrincipal User u, Pageable p) {
        return service.hodLists(u, p);
    }

    @GetMapping("/hod/lists/{listId}/entries")
    @PreAuthorize("hasRole('HOD')")
    public Page<GraduationCandidateDto> hodListEntries(@AuthenticationPrincipal User u, @PathVariable UUID listId, @RequestParam(required = false) String search, Pageable p) {
        return service.hodListEntries(u, listId, search, p);
    }

    @DeleteMapping("/hod/lists/{listId}/entries/{enrollmentId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('HOD')")
    public void removeListEntry(@AuthenticationPrincipal User u, @PathVariable UUID listId, @PathVariable UUID enrollmentId) {
        service.removeListEntry(u, listId, enrollmentId);
    }

    @GetMapping(value = "/hod/lists/{listId}/pdf", produces = "application/pdf")
    @PreAuthorize("hasRole('HOD')")
    public byte[] hodListPdf(@AuthenticationPrincipal User u, @PathVariable UUID listId) {
        return service.hodListPdf(u, listId);
    }

    @GetMapping("/hod")
    @PreAuthorize("hasRole('HOD')")
    public Summary hod(@AuthenticationPrincipal User u, @RequestParam UUID academicYearUuid) {
        return service.hodList(u, academicYearUuid);
    }

    @PostMapping("/hod/publish")
    @PreAuthorize("hasRole('HOD')")
    public Summary publish(@AuthenticationPrincipal User u, @RequestBody YearRequest r) {
        return service.publish(u, r.academicYearUuid());
    }

    @PostMapping("/hod/unpublish")
    @PreAuthorize("hasRole('HOD')")
    public Summary unpublish(@AuthenticationPrincipal User u, @RequestBody YearRequest r) {
        return service.unpublish(u, r.academicYearUuid());
    }

    @PostMapping("/hod/submit")
    @PreAuthorize("hasRole('HOD')")
    public Summary submit(@AuthenticationPrincipal User u, @RequestBody YearRequest r) {
        return service.submit(u, r.academicYearUuid());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public GraduationCandidateDto mine(@AuthenticationPrincipal User u) {
        return service.mine(u);
    }

    @PostMapping("/me/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public GraduationCandidateDto confirm(@AuthenticationPrincipal User u) {
        return service.confirm(u);
    }

    @PatchMapping("/me/details")
    @PreAuthorize("hasRole('STUDENT')")
    public GraduationCandidateDto updateDetails(@AuthenticationPrincipal User u, @RequestBody UpdateDetailsRequest r) {
        return service.updateDetails(u, r);
    }

    @GetMapping("/staff/lists")
    @PreAuthorize("hasAnyRole('FINANCE','REGISTRAR','ADMIN')")
    public Page<Summary> staffLists(@AuthenticationPrincipal User user, @RequestParam(required = false) UUID academicYearUuid, @RequestParam(required = false) Long departmentId, Pageable p) {
        return service.staffLists(user, academicYearUuid, departmentId, p);
    }

    @GetMapping("/staff/lists/{listId}/candidates")
    @PreAuthorize("hasAnyRole('FINANCE','REGISTRAR','ADMIN')")
    public Page<CandidateRow> staffCandidates(@AuthenticationPrincipal User user, @PathVariable UUID listId,
                                              @RequestParam(required = false) String search, Pageable p) {
        return service.staffListCandidates(user, listId, search, p);
    }

    @GetMapping("/staff/entries/{id}")
    @PreAuthorize("hasAnyRole('FINANCE','REGISTRAR','ADMIN')")
    public GraduationCandidateDto staffEntry(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.staffEntry(user, id);
    }

    @GetMapping("/registrar/entries")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Page<GraduationCandidateDto> entries(@RequestParam(required = false) String search, @RequestParam(required = false) String status, @RequestParam(required = false) UUID academicYearUuid, Pageable p) {
        return service.registrarEntries(search, status, academicYearUuid, p);
    }

    @GetMapping("/registrar/final-lists")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Page<Summary> finalLists(@RequestParam(required = false) UUID academicYearUuid,
                                    @RequestParam(required = false) Long departmentId, Pageable pageable) {
        return service.registrarFinalLists(academicYearUuid, departmentId, pageable);
    }

    @GetMapping("/registrar/final-lists/{listId}/candidates")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Page<CandidateRow> finalListCandidates(@PathVariable UUID listId,
                                                  @RequestParam(required = false) String search, Pageable pageable) {
        return service.registrarFinalCandidates(listId, search, pageable);
    }

    @GetMapping(value = "/registrar/final-lists/{listId}/pdf", produces = "application/pdf")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public byte[] finalListPdf(@PathVariable UUID listId) {
        return service.registrarFinalListPdf(listId);
    }

    @PostMapping("/registrar/entries/{id}/approve")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public GraduationCandidateDto approve(@AuthenticationPrincipal User u, @PathVariable Long id) {
        return service.approve(u, id);
    }

    @PostMapping(value = "/registrar/entries/{id}/certificate", produces = "application/pdf")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public byte[] certificate(@AuthenticationPrincipal User registrar, @PathVariable Long id) {
        return service.printCertificate(registrar, id);
    }

    @GetMapping(value = "/registrar/booklet", produces = "application/pdf")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public ResponseEntity<byte[]> booklet(@RequestParam UUID academicYearUuid) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=final-graduation-booklet.pdf").body(service.finalBooklet(academicYearUuid));
    }
}
