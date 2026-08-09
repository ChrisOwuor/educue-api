package com.owuor.educue.graduation.service;

import com.owuor.educue.graduation.dto.GraduationBatchDtos.*;
import com.owuor.educue.graduation.entity.*;
import com.owuor.educue.graduation.enums.*;
import com.owuor.educue.graduation.repository.*;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.users.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GraduationBatchService {
    private final GraduationBatchRepository batchRepository;
    private final GraduationBatchCandidateRepository candidateRepository;
    private final GraduationApplicationRepository applicationRepository;
    private final GraduationApplicationService applicationService;
    private final AcademicYearRepository yearRepository;
    private final GraduationBookletPdfService bookletPdfService;
    private final ApplicationEventPublisher events;

    @Transactional public Summary create(CreateRequest request,User user){var year=yearRepository.findByUuid(request.academicYearUuid()).orElseThrow(()->notFound("Academic year not found"));Specification<GraduationApplication> spec=(root,query,cb)->cb.and(cb.equal(root.get("graduationList").get("academicYear").get("id"),year.getId()),cb.equal(root.get("status"),GraduationApplicationStatus.APPROVED_FOR_GRADUATION));var eligible=applicationRepository.findAll(spec,PageRequest.of(0,10000));if(eligible.isEmpty())throw conflict("No approved final-list candidates are available");GraduationBatch batch=new GraduationBatch();batch.setName(request.name().trim());batch.setGraduationDate(request.graduationDate());batch.setAcademicYear(year);batch.setCreatedBy(user);batch=batchRepository.save(batch);for(var application:eligible){var candidate=new GraduationBatchCandidate();candidate.setBatch(batch);candidate.setApplication(application);candidateRepository.save(candidate);application.setStatus(GraduationApplicationStatus.ASSIGNED_TO_BATCH);application.setGraduationBatch(batch);}batch.setTotalCandidates(eligible.getNumberOfElements());return summary(batchRepository.save(batch));}

    @Transactional
    public Page<Summary> list(Pageable pageable) {
        return batchRepository.findAllByOrderByGraduationDateDesc(pageable).map(this::summary);
    }

    @Transactional
    public Detail detail(UUID uuid) {
        return new Detail(summary(batch(uuid)));
    }

    @Transactional
    public Page<Candidate> candidates(UUID uuid, String search, Long courseId, Long departmentId, Pageable pageable) {
        var batch = batch(uuid);
        Specification<GraduationBatchCandidate> spec=(root,query,cb)->cb.equal(root.get("batch").get("id"),batch.getId());
        if(courseId!=null)spec=spec.and((root,query,cb)->cb.equal(root.get("application").get("enrollment").get("course").get("id"),courseId));
        if(departmentId!=null)spec=spec.and((root,query,cb)->cb.equal(root.get("application").get("enrollment").get("course").get("department").get("id"),departmentId));
        if(search!=null&&!search.isBlank()){String value="%"+search.trim().toLowerCase()+"%";spec=spec.and((root,query,cb)->cb.or(cb.like(cb.lower(root.get("application").get("enrollment").get("student").get("fullName")),value),cb.like(cb.lower(root.get("application").get("enrollment").get("student").get("admissionNumber")),value)));}
        return candidateRepository.findAll(spec,pageable).map(this::candidate);
    }

    @Transactional
    public void removeCandidate(UUID uuid, Long candidateId) {
        var batch = batch(uuid);
        if (batch.getStatus() != GraduationBatchStatus.DRAFT)
            throw conflict("Candidates can only be removed from a draft batch");
        var candidate = candidateRepository.findById(candidateId).orElseThrow(() -> notFound("Batch candidate not found"));
        if (!candidate.getBatch().getId().equals(batch.getId())) throw notFound("Batch candidate not found");
        candidateRepository.delete(candidate);
        batch.setTotalCandidates(Math.max(0, batch.getTotalCandidates() - 1));
        batchRepository.save(batch);
    }

    @Transactional
    public Summary confer(UUID uuid, User user) {
        var batch = batch(uuid);
        if (batch.getStatus() != GraduationBatchStatus.DRAFT && batch.getStatus() != GraduationBatchStatus.COMPLETED_WITH_ERRORS)
            throw conflict("This batch cannot be conferred in its current status");
//        if (batch.getGraduationDate().isAfter(LocalDate.now()))
//            throw conflict("Awards can only be conferred on or after the graduation date");
        if (batch.getTotalCandidates() == 0) throw conflict("The batch has no candidates");
        batch.setStatus(GraduationBatchStatus.PROCESSING);
        batch.setConferredBy(user);
        batch.setConferredAt(LocalDateTime.now());
        batchRepository.save(batch);
        events.publishEvent(new ConferBatchEvent(batch.getId(), user.getId()));
        return summary(batch);
    }

    @Transactional
    public byte[] booklet(UUID uuid) {
        var batch = batch(uuid);
        var candidates = candidateRepository.findByBatchIdOrderByApplicationEnrollmentCourseDepartmentNameAscApplicationEnrollmentCourseNameAscApplicationEnrollmentStudentFullNameAsc(batch.getId());
        return bookletPdfService.generate(batch,candidates);
    }

    private GraduationBatch batch(UUID uuid) {
        return batchRepository.findByUuid(uuid).orElseThrow(() -> notFound("Graduation batch not found"));
    }

    private Summary summary(GraduationBatch b) {
        return new Summary(b.getUuid(), b.getName(), b.getGraduationDate(), b.getAcademicYear().getCode(), b.getStatus().name(), b.getTotalCandidates(), b.getProcessedCandidates(), b.getSuccessfulCandidates(), b.getFailedCandidates(), b.getCreatedBy().getFullName(), b.getCreatedAt(), b.getConferredAt());
    }

    private Candidate candidate(GraduationBatchCandidate c) {
        var e = c.getApplication().getEnrollment();
        return new Candidate(c.getId(), c.getApplication().getId(), e.getStudent().getAdmissionNumber(), e.getStudent().getFullName(), e.getCourse().getCode(), e.getCourse().getName(), e.getCourse().getDepartment().getName(), c.getStatus().name(), c.getFailureReason(), c.getProcessedAt());
    }

    private ResponseStatusException conflict(String m) {
        return new ResponseStatusException(HttpStatus.CONFLICT, m);
    }

    private ResponseStatusException notFound(String m) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
    }

    public record ConferBatchEvent(Long batchId, Long userId) {
    }
}
