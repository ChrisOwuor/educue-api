package com.owuor.educue.certificate;

import com.owuor.educue.certificate.dto.CertificateDtos.*;
import com.owuor.educue.certificate.service.CertificateJobService;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/certificate-jobs")
@RequiredArgsConstructor
public class CertificateJobController {
    private final CertificateJobService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Job create(@Valid @RequestBody CreateJobRequest request, @AuthenticationPrincipal User user) {
        return service.create(request, user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Page<Job> jobs(Pageable pageable) { return service.jobs(pageable); }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Job job(@PathVariable UUID uuid) { return service.job(uuid); }

    @GetMapping("/{uuid}/certificates")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Page<Certificate> certificates(@PathVariable UUID uuid, @RequestParam(required=false) String search,
                                          @RequestParam(required=false) String status, Pageable pageable) {
        return service.certificates(uuid, search, status, pageable);
    }

    @PostMapping("/certificates/{uuid}/retry")
    @PreAuthorize("hasAnyRole('REGISTRAR','ADMIN')")
    public Certificate retry(@PathVariable UUID uuid) { return service.retry(uuid); }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public Certificate mine(@AuthenticationPrincipal User user) { return service.mine(user); }
}
