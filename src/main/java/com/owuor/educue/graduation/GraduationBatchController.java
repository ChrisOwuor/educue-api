package com.owuor.educue.graduation;

import com.owuor.educue.graduation.dto.GraduationBatchDtos.*;
import com.owuor.educue.graduation.service.GraduationBatchService;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/graduation-batches")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
public class GraduationBatchController {
    private final GraduationBatchService service;

    @PostMapping public Summary create(@Valid @RequestBody CreateRequest request, @AuthenticationPrincipal User user) { return service.create(request,user); }
    @GetMapping public Page<Summary> list(Pageable pageable) { return service.list(pageable); }
    @GetMapping("/{uuid}") public Detail detail(@PathVariable UUID uuid) { return service.detail(uuid); }
    @GetMapping("/{uuid}/candidates") public Page<Candidate> candidates(@PathVariable UUID uuid,@RequestParam(required=false) String search,@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long departmentId,Pageable pageable){return service.candidates(uuid,search,courseId,departmentId,pageable);}
    @DeleteMapping("/{uuid}/candidates/{candidateId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable UUID uuid,@PathVariable Long candidateId){service.removeCandidate(uuid,candidateId);}
    @PostMapping("/{uuid}/confer") public Summary confer(@PathVariable UUID uuid,@AuthenticationPrincipal User user){return service.confer(uuid,user);}
    @GetMapping(value="/{uuid}/booklet.pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> booklet(@PathVariable UUID uuid){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=graduation-booklet.pdf").body(service.booklet(uuid));}
}
