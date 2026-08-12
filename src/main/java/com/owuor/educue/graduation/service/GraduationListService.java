package com.owuor.educue.graduation.service;

import com.owuor.educue.graduation.dto.GraduationListDtos.*;
import com.owuor.educue.graduation.dto.GraduationReadinessResponse;
import com.owuor.educue.graduation.entity.*;
import com.owuor.educue.graduation.enums.*;
import com.owuor.educue.graduation.repository.*;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.enums.EnrollmentStatus;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.results.service.StudentResultService;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.certificate.entity.GraduationCertificate;
import com.owuor.educue.certificate.service.CertificatePdfRenderer;
import com.owuor.educue.clearance.repository.ClearanceApplicationRepository;
import com.owuor.educue.finance.repository.FeeLedgerRepository;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GraduationListService {
    private final EnrollmentRepository enrollments;
    private final GraduationCandidateRepository graduationCandidateRepository;
    private final GraduationListRepository graduationListRepository;
    private final AcademicYearRepository years;
    private final FinalGraduationBookletPdfService finalBookletPdf;
    private final GraduationReadinessService readiness;
    private final GraduationFeeService graduationFees;
    private final FeeLedgerService ledgerService;
    private final FeeLedgerRepository ledgers;
    private final StudentResultService resultService;
    private final CertificatePdfRenderer certificateRenderer;
    private final ClearanceApplicationRepository clearanceApplications;
    private final com.owuor.educue.clearance.repository.ClearanceCheckRepository clearanceChecks;
    private final StudentResultRepository resultRepository;
    private final ProvisionalGraduationListPdfRenderer graduationListPdfRenderer;

    @Transactional(readOnly = true)
    public Page<Candidate> hodEnrollments(User hod, String search, UUID academicPeriodUuid, boolean finalistsOnly, Pageable pageable) {
        Specification<Enrollment> specification = departmentScope(department(hod));
        if (search != null && !search.isBlank()) {
            String value = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("student").get("fullName")), value),
                    builder.like(builder.lower(root.get("student").get("admissionNumber")), value),
                    builder.like(builder.lower(root.get("course").get("name")), value)));
        }
        if (academicPeriodUuid != null)
            specification = specification.and((root, query, builder) -> builder.equal(
                    root.get("currentCourseAcademicPeriod").get("academicPeriod").get("uuid"), academicPeriodUuid));
        if (finalistsOnly)
            specification = specification.and((root, query, builder) -> builder.isNull(
                    root.get("currentCourseAcademicPeriod").get("nextPeriod")));
        return enrollments.findAll(specification, pageable).map(enrollment -> new Candidate(
                enrollment.getUuid(),
                enrollment.getStudent().getAdmissionNumber(), enrollment.getCourse().getCode(), enrollment.getCourse().getName(),
                enrollment.getCurrentCourseAcademicPeriod().getAcademicPeriod().getCode(),
                enrollment.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName(), isFinal(enrollment),
                false, false, null, enrollment.getStatus().name()));
    }

    @Transactional
    public GraduationCandidateDto assess(User hod, UUID yearUuid, UUID enrollmentId) {
        Enrollment enrollment = owned(hod, yearUuid, enrollmentId);
        var selectedGraduationList = graduationListRepository.findByAcademicYearIdAndDepartmentId(year(yearUuid).getId(), department(hod)).orElse(null);
        GraduationCandidate existingCandidate = selectedGraduationList == null ? null
                : graduationCandidateRepository.findByGraduationListIdAndEnrollmentId(selectedGraduationList.getId(), enrollment.getId()).orElse(null);
        if (existingCandidate != null) {
            if (existingCandidate.getReadiness() == null) {
                applyAcademicSnapshot(existingCandidate, enrollment);
                return response(graduationCandidateRepository.save(existingCandidate));
            }
            return response(existingCandidate);
        }
        return transientEntry(enrollment, year(yearUuid), null);
    }

    @Transactional
    public GraduationCandidateDto enrollmentDetail(User hod, UUID enrollmentId) {
        var e = enrollments.findByUuid(enrollmentId).orElseThrow(() -> notFound("Enrollment not found"));
        if (!ownedByDepartment(e, department(hod))) throw notFound("Enrollment not found");
        var yearUuid = e.getCurrentAcademicYear().getUuid();
        var selectedGraduationList = graduationListRepository.findByAcademicYearIdAndDepartmentId(year(yearUuid).getId(), department(hod)).orElse(null);
        var existing = selectedGraduationList == null ? null : graduationCandidateRepository.findByGraduationListIdAndEnrollmentId(selectedGraduationList.getId(), e.getId()).orElse(null);
        // A persisted candidate already owns an assessment snapshot. Opening the
        // detail must never silently replace it; the HOD sees stored results and
        // can explicitly clear/reassess the snapshot if necessary.
        if (existing != null) return response(existing);
        return isFinal(e) ? transientEntry(e, year(yearUuid), null) : basicEntry(e, year(yearUuid));
    }

    @Transactional
    public void clearAssessment(User hod, UUID enrollmentId) {
        Enrollment enrollment = enrollments.findByUuid(enrollmentId)
                .orElseThrow(() -> notFound("Enrollment not found"));
        if (!ownedByDepartment(enrollment, department(hod))) throw notFound("Enrollment not found");
        GraduationCandidate candidate = graduationCandidateRepository.findFirstByEnrollmentIdOrderByGraduationListAcademicYearStartDateDesc(enrollment.getId())
                .orElseThrow(() -> notFound("Graduation candidate not found"));
        if (!"DRAFT".equals(candidate.getGraduationList().getStatus()))
            throw conflict("Only an assessment on a draft graduation list can be cleared");
        candidate.setReadiness(null);
        candidate.setFinalCumulativeAverage(null);
        candidate.setAwardClassification(null);
        graduationCandidateRepository.save(candidate);
    }

    @Transactional
    public GraduationCandidateDto add(User hod, AddRequest req) {
        var e = owned(hod, req.academicYearUuid(), req.enrollmentUuid());
        if (e.getStatus() == EnrollmentStatus.GRADUATED || graduationCandidateRepository.existsByEnrollmentIdAndStatusIn(e.getId(), List.of(GraduationCandidateStatus.GRADUATED, GraduationCandidateStatus.CONFERRED)))
            throw conflict("This student has already graduated and cannot be added to another graduation list");
        if (graduationCandidateRepository.findByGraduationListIdAndEnrollmentId(list(req.academicYearUuid(), hod).getId(), e.getId()).isPresent())
            throw conflict("Student is already on a graduation list");
        GraduationCandidateDto assessment = transientEntry(e, year(req.academicYearUuid()), req.remarks());
        if (!assessment.eligible()) throw conflict("The student is not academically eligible for graduation");
        var list = graduationListRepository.findByAcademicYearIdAndDepartmentId(year(req.academicYearUuid()).getId(), department(hod)).orElseThrow(() -> conflict("Create the departmental graduation list before adding candidates"));
        if (!"DRAFT".equals(list.getStatus())) throw conflict("Only a draft graduation list can be changed");
        var a = new GraduationCandidate();
        a.setGraduationList(list);
        a.setEnrollment(e);
        a.setStatus(GraduationCandidateStatus.NOT_STARTED);
        a.setAdmissionNumberSnapshot(e.getStudent().getAdmissionNumber());
        a.setCourseCodeSnapshot(e.getCourse().getCode());
        a.setCourseNameSnapshot(e.getCourse().getName());
        a.setGraduationName(e.getStudent().getFullName());
        a.setAwardTitle(require(e.getCourse().getAwardTitle(), "Award title is not configured"));
        a.setQualificationType(e.getCourse().getQualificationType());
        a.setHodRemarks(blank(req.remarks()));
        applyAcademicSnapshot(a, e);
        return response(graduationCandidateRepository.save(a));
    }

    @Transactional
    public Summary publish(User hod, UUID yearUuid) {
        var l = list(yearUuid, hod);
        if (!"DRAFT".equals(l.getStatus())) throw conflict("Only a draft list can be published");
        if (graduationCandidateRepository.count((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId())) == 0)
            throw conflict("Add at least one eligible candidate first");
        l.setStatus("PROVISIONAL");
        l.setPublishedBy(hod);
        l.setPublishedAt(LocalDateTime.now());
        graduationCandidateRepository.findAll((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId())).forEach(a -> a.setStatus(GraduationCandidateStatus.PROVISIONAL));
        return summary(graduationListRepository.save(l));
    }

    @Transactional
    public Summary unpublish(User hod, UUID yearUuid) {
        var list = list(yearUuid, hod);
        if (!"PROVISIONAL".equals(list.getStatus()))
            throw conflict("Only a provisional graduation list can be unpublished");
        var candidates = graduationCandidateRepository.findAll((r, q, b) -> b.equal(r.get("graduationList").get("id"), list.getId()));
        boolean studentActionStarted = candidates.stream().anyMatch(candidate ->
                candidate.getDetailsConfirmedAt() != null ||
                candidate.getStatus() != GraduationCandidateStatus.PROVISIONAL);
        if (studentActionStarted)
            throw conflict("This list cannot be unpublished because a candidate has already confirmed details or started graduation processing");
        candidates.forEach(candidate -> candidate.setStatus(GraduationCandidateStatus.NOT_STARTED));
        list.setStatus("DRAFT");
        list.setPublishedBy(null);
        list.setPublishedAt(null);
        return summary(graduationListRepository.save(list));
    }

    @Transactional
    public Summary submit(User hod, UUID yearUuid) {
        var l = list(yearUuid, hod);
        if (!"PROVISIONAL".equals(l.getStatus())) throw conflict("Publish the provisional list before submitting it");
        l.setStatus("SUBMITTED");
        l.setSubmittedBy(hod);
        l.setSubmittedAt(LocalDateTime.now());
        return summary(graduationListRepository.save(l));
    }

    @Transactional(readOnly = true)
    public Summary hodList(User hod, UUID yearUuid) {
        var y = year(yearUuid);
        return graduationListRepository.findByAcademicYearIdAndDepartmentId(y.getId(), department(hod)).map(this::summary).orElse(new Summary(null, y.getUuid(), y.getCode(), hod.getDepartment().getName(), "DRAFT", 0, 0, null, null));
    }

    @Transactional(readOnly = true)
    public GraduationCandidateDto mine(User user) {
        var e = enrollments.findByStudentUserId(user.getId()).orElseThrow(() -> notFound("Student enrollment not found"));
        var a = graduationCandidateRepository.findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(e.getId(), List.of(GraduationCandidateStatus.DRAFT, GraduationCandidateStatus.REMOVED)).orElseThrow(() -> notFound("Student is not on a published graduation list"));
        if (a.getStatus() == GraduationCandidateStatus.DRAFT)
            throw notFound("Student is not on a published graduation list");
        return response(a);
    }

    @Transactional
    public GraduationCandidateDto updateDetails(User user, UpdateDetailsRequest request) {
        var e = enrollments.findByStudentUserIdForUpdate(user.getId()).orElseThrow(() -> notFound("Student enrollment not found"));
        var a = graduationCandidateRepository.findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(e.getId(), List.of(GraduationCandidateStatus.DRAFT, GraduationCandidateStatus.REMOVED)).orElseThrow(() -> notFound("Student is not on a published graduation list"));
        if (a.getDetailsConfirmedAt() != null) throw conflict("Graduation details are already confirmed");
        String name = require(request.graduationName(), "Graduation name is required");
        if (name.length() > 180) throw conflict("Graduation name cannot exceed 180 characters");
        a.setGraduationName(name);
        return response(graduationCandidateRepository.save(a));
    }

    @Transactional
    public GraduationCandidateDto confirm(User user) {
        var e = enrollments.findByStudentUserIdForUpdate(user.getId()).orElseThrow(() -> notFound("Student enrollment not found"));
        var a = graduationCandidateRepository.findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(e.getId(), List.of(GraduationCandidateStatus.DRAFT, GraduationCandidateStatus.REMOVED)).orElseThrow(() -> notFound("Student is not on a published graduation list"));
        if (a.getDetailsConfirmedAt() != null) return response(a);
        if (a.getStatus() != GraduationCandidateStatus.PROVISIONAL && a.getStatus() != GraduationCandidateStatus.SUBMITTED_TO_REGISTRAR)
            throw conflict("Graduation details are not open for confirmation");
        if (!graduationFeeCharged(a)) ledgerService.billGraduation(e, graduationFee(a));
        a.setDetailsConfirmedAt(LocalDateTime.now());
        a.setStatus(GraduationCandidateStatus.DETAILS_CONFIRMED);
        return response(graduationCandidateRepository.save(a));
    }

    @Transactional
    public Summary createList(User hod, UUID yearUuid) {
        var y = year(yearUuid);
        if (graduationListRepository.findByAcademicYearIdAndDepartmentId(y.getId(), department(hod)).isPresent())
            throw conflict("A departmental graduation list already exists for this academic year");
        var l = new GraduationList();
        l.setAcademicYear(y);
        l.setDepartment(hod.getDepartment());
        return summary(graduationListRepository.save(l));
    }

    @Transactional
    public Summary updateList(User hod, UUID listId, UUID yearUuid) {
        var list = ownedList(hod, listId);
        if (!"DRAFT".equals(list.getStatus()))
            throw conflict("Only a draft graduation list can be edited");
        var targetYear = year(yearUuid);
        graduationListRepository.findByAcademicYearIdAndDepartmentId(targetYear.getId(), department(hod))
                .filter(existing -> !existing.getId().equals(list.getId()))
                .ifPresent(existing -> {
                    throw conflict("A departmental graduation list already exists for this academic year");
                });
        list.setAcademicYear(targetYear);
        return summary(graduationListRepository.save(list));
    }

    @Transactional
    public void deleteList(User hod, UUID listId) {
        var list = ownedList(hod, listId);
        if (!"DRAFT".equals(list.getStatus()))
            throw conflict("Only a draft graduation list can be deleted");
        long candidates = graduationCandidateRepository.count((r, q, b) -> b.equal(r.get("graduationList").get("id"), list.getId()));
        if (candidates > 0)
            throw conflict("Remove all candidates before deleting this graduation list");
        graduationListRepository.delete(list);
    }

    @Transactional(readOnly = true)
    public Page<Summary> hodLists(User hod, Pageable pageable) {
        Specification<GraduationList> s = (r, q, b) -> b.equal(r.get("department").get("id"), department(hod));
        return graduationListRepository.findAll(s, pageable).map(this::summary);
    }

    @Transactional(readOnly = true)
    public Page<GraduationCandidateDto> hodListEntries(User hod, UUID listId, String search, Pageable pageable) {
        var l = ownedList(hod, listId);
        Specification<GraduationCandidate> s = (r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId());
        if (search != null && !search.isBlank()) {
            String v = "%" + search.trim().toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("graduationName")), v), b.like(b.lower(r.get("admissionNumberSnapshot")), v)));
        }
        return graduationCandidateRepository.findAll(s, pageable).map(this::response);
    }

    @Transactional
    public void removeListEntry(User hod, UUID listId, UUID enrollmentUuid) {
        var list = ownedList(hod, listId);
        if (!"DRAFT".equals(list.getStatus()))
            throw conflict("Candidates can only be removed from a draft graduation list");
        var enrollment = enrollments.findByUuid(enrollmentUuid)
                .orElseThrow(() -> notFound("Candidate was not found on this graduation list"));
        var candidate = graduationCandidateRepository.findByGraduationListIdAndEnrollmentId(list.getId(), enrollment.getId())
                .orElseThrow(() -> notFound("Candidate was not found on this graduation list"));
        if (candidate.getDetailsConfirmedAt() != null || graduationFeeCharged(candidate))
            throw conflict("A candidate with confirmed details or graduation charges cannot be removed");
        graduationCandidateRepository.delete(candidate);
    }

    @Transactional(readOnly = true)
    public byte[] hodListPdf(User hod, UUID listId) {
        var l = ownedList(hod, listId);
        var rows = graduationCandidateRepository.findAll((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId()), Sort.by("graduationName"));
        return graduationListPdfRenderer.render(l, rows);
    }

    private GraduationList ownedList(User hod, UUID id) {
        var l = graduationListRepository.findByUuid(id).orElseThrow(() -> notFound("Graduation list not found"));
        if (!l.getDepartment().getId().equals(department(hod)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Graduation list belongs to another department");
        return l;
    }

    @Transactional(readOnly = true)
    public Page<Summary> staffLists(User user, UUID yearUuid, Long departmentId, Pageable pageable) {
        Specification<GraduationList> s = (r, q, b) -> b.equal(r.get("status"), "SUBMITTED");
        if (yearUuid != null) s = s.and((r, q, b) -> b.equal(r.get("academicYear").get("uuid"), yearUuid));
        if (departmentId != null) s = s.and((r, q, b) -> b.equal(r.get("department").get("id"), departmentId));
        return graduationListRepository.findAll(s, pageable).map(this::summary);
    }

    @Transactional(readOnly = true)
    public Page<Summary> registrarFinalLists(UUID academicYearUuid, Long departmentId, Pageable pageable) {
        Specification<GraduationList> specification = (root, query, builder) -> {
            var approvedCandidate = query.subquery(Long.class);
            var candidateRoot = approvedCandidate.from(GraduationCandidate.class);
            approvedCandidate.select(candidateRoot.get("id")).where(
                    builder.equal(candidateRoot.get("graduationList").get("id"), root.get("id")),
                    builder.equal(candidateRoot.get("status"), GraduationCandidateStatus.APPROVED_FOR_GRADUATION));
            return builder.exists(approvedCandidate);
        };
        if (academicYearUuid != null)
            specification = specification.and((root, query, builder) -> builder.equal(root.get("academicYear").get("uuid"), academicYearUuid));
        if (departmentId != null)
            specification = specification.and((root, query, builder) -> builder.equal(root.get("department").get("id"), departmentId));
        return graduationListRepository.findAll(specification, pageable).map(this::finalSummary);
    }

    @Transactional(readOnly = true)
    public Page<CandidateRow> registrarFinalCandidates(UUID listUuid, String search, Pageable pageable) {
        GraduationList graduationList = graduationListRepository.findByUuid(listUuid)
                .orElseThrow(() -> notFound("Graduation list not found"));
        Specification<GraduationCandidate> specification = (root, query, builder) -> builder.and(
                builder.equal(root.get("graduationList").get("id"), graduationList.getId()),
                builder.equal(root.get("status"), GraduationCandidateStatus.APPROVED_FOR_GRADUATION));
        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("graduationName")), searchPattern),
                    builder.like(builder.lower(root.get("admissionNumberSnapshot")), searchPattern)));
        }
        return graduationCandidateRepository.findAll(specification, pageable).map(this::candidateRow);
    }

    @Transactional(readOnly = true)
    public byte[] registrarFinalListPdf(UUID listUuid) {
        GraduationList graduationList = graduationListRepository.findByUuid(listUuid)
                .orElseThrow(() -> notFound("Graduation list not found"));
        List<GraduationCandidate> approvedCandidates = graduationCandidateRepository.findAll(
                (root, query, builder) -> builder.and(
                        builder.equal(root.get("graduationList").get("id"), graduationList.getId()),
                        builder.equal(root.get("status"), GraduationCandidateStatus.APPROVED_FOR_GRADUATION)),
                Sort.by("graduationName"));
        if (approvedCandidates.isEmpty()) throw notFound("This final list has no approved candidates");
        return graduationListPdfRenderer.renderFinal(graduationList, approvedCandidates);
    }

    @Transactional(readOnly = true)
    public Page<CandidateRow> staffListCandidates(User user, UUID listId, String search, Pageable pageable) {
        var selectedGraduationList = graduationListRepository.findByUuid(listId).orElseThrow(() -> notFound("Graduation list not found"));
        Specification<GraduationCandidate> s = (r, q, b) -> b.equal(r.get("graduationList").get("id"), selectedGraduationList.getId());
        s = staffStage(user, s);
        if (search != null && !search.isBlank()) {
            String value = "%" + search.trim().toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("graduationName")), value),
                    b.like(b.lower(r.get("admissionNumberSnapshot")), value)));
        }
        return graduationCandidateRepository.findAll(s, pageable).map(this::candidateRow);
    }

    private List<AcademicResult> academicResults(GraduationCandidate a) {
        return resultRepository.findByStudentUnitRegistrationEnrollmentId(a.getEnrollment().getId()).stream().sorted(Comparator.comparing(x -> x.getStudentUnitRegistration().getCourseUnitPlacement().getCourseAcademicPeriod().getPosition())).map(r -> {
            var p = r.getStudentUnitRegistration().getCourseUnitPlacement();
            var period = p.getCourseAcademicPeriod().getAcademicPeriod();
            var unit = p.getUnit();
            return new AcademicResult(r.getId(), period.getCode(), period.getName(), unit.getCode(), unit.getName(), unit.getCreditHours(), r.getStudentUnitRegistration().getAttemptType().name(), r.getCaMarks(), r.getExamMarks(), r.getTotalMarks(), r.getGrade(), r.isPassed(), r.getStatus().name());
        }).toList();
    }

    private List<ClearanceItem> clearanceItems(GraduationCandidate a) {
        return clearanceApplications.findByEnrollmentIdAndAcademicYearId(a.getEnrollment().getId(), a.getGraduationList().getAcademicYear().getId()).map(ca -> ca.getChecks().stream().map(c -> new ClearanceItem(c.getId(), c.getDepartment().getName(), c.getStatus().name(), c.getRemarks(), c.getReviewedBy() == null ? null : c.getReviewedBy().getFullName(), c.getReviewedAt(), c.getStage() == com.owuor.educue.clearance.enums.ClearanceDepartmentStage.FINANCE)).toList()).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Page<GraduationCandidateDto> registrarEntries(String search, String status, UUID yearUuid, Pageable pageable) {
        Specification<GraduationCandidate> s = (r, q, b) -> b.equal(r.get("graduationList").get("status"), "SUBMITTED");
        if (yearUuid != null)
            s = s.and((r, q, b) -> b.equal(r.get("graduationList").get("academicYear").get("uuid"), yearUuid));
        if (status != null && !status.isBlank()) {
            var x = GraduationCandidateStatus.valueOf(status.toUpperCase());
            s = s.and((r, q, b) -> b.equal(r.get("status"), x));
        } else s = s.and((r, q, b) -> b.equal(r.get("clearanceStage"), GraduationClearanceStage.REGISTRAR_REVIEW));
        if (search != null && !search.isBlank()) {
            String v = "%" + search.toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("admissionNumberSnapshot")), v), b.like(b.lower(r.get("enrollment").get("student").get("fullName")), v)));
        }
        return graduationCandidateRepository.findAll(s, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public GraduationCandidateDto staffEntry(User user, Long id) {
        var candidate = graduationCandidateRepository.findDetailedById(id).orElseThrow(() -> notFound("Graduation candidate not found"));
        String role = user.getRole().getName();
        if ("FINANCE".equals(role) && candidate.getClearanceStage() != GraduationClearanceStage.FINANCE_REVIEW)
            throw notFound("Candidate has not reached Finance review");
        if ("REGISTRAR".equals(role) && candidate.getClearanceStage() != GraduationClearanceStage.REGISTRAR_REVIEW && candidate.getStatus() != GraduationCandidateStatus.APPROVED_FOR_GRADUATION)
            throw notFound("Candidate has not reached Registrar review");
        return response(candidate);
    }

    private Specification<GraduationCandidate> staffStage(User user, Specification<GraduationCandidate> specification) {
        String role = user.getRole().getName();
        if ("FINANCE".equals(role))
            return specification.and((r, q, b) -> b.equal(r.get("clearanceStage"), GraduationClearanceStage.FINANCE_REVIEW));
        if ("REGISTRAR".equals(role)) return specification.and((r, q, b) ->
                b.equal(r.get("clearanceStage"), GraduationClearanceStage.REGISTRAR_REVIEW));
        return specification;
    }

    @Transactional
    public byte[] printCertificate(User registrar, Long candidateId) {
        GraduationCandidate candidate = graduationCandidateRepository.findDetailedByIdForUpdate(candidateId)
                .orElseThrow(() -> notFound("Graduation candidate not found"));
        if (candidate.getStatus() != GraduationCandidateStatus.APPROVED_FOR_GRADUATION)
            throw conflict("Only approved-for-graduation candidates can receive certificates");
        if (candidate.isCertificatePrinted())
            throw conflict("This certificate was already printed on " + candidate.getCertificatePrintedAt());

        String certificateNumber = "GRAD-" + candidate.getGraduationList().getAcademicYear().getStartYear()
                + "-" + String.format("%06d", candidate.getId());
        GraduationCertificate certificate = new GraduationCertificate();
        certificate.setStudentName(candidate.getGraduationName());
        certificate.setAdmissionNumber(candidate.getAdmissionNumberSnapshot());
        certificate.setCourseCode(candidate.getCourseCodeSnapshot());
        certificate.setCourseName(candidate.getCourseNameSnapshot());
        certificate.setAwardTitle(candidate.getAwardTitle());
        certificate.setAwardClassification(candidate.getAwardClassification() == null ? null : candidate.getAwardClassification().getDisplayName());
        certificate.setGraduationDate(Optional.ofNullable(candidate.getGraduationList().getGraduationDate())
                .orElse(candidate.getGraduationList().getAcademicYear().getEndDate()));
        certificate.setCertificateNumber(certificateNumber);

        byte[] certificatePdf = certificateRenderer.render(certificate);
        candidate.setCertificateNumber(certificateNumber);
        candidate.setCertificatePrinted(true);
        candidate.setCertificatePrintedAt(LocalDateTime.now());
        candidate.setCertificatePrintedBy(registrar);
        graduationCandidateRepository.save(candidate);
        return certificatePdf;
    }

    @Transactional(readOnly = true)
    public byte[] finalBooklet(UUID academicYearUuid) {
        var year = years.findByUuid(academicYearUuid).orElseThrow(() -> notFound("Academic year not found"));
        var candidates = graduationCandidateRepository.findByGraduationListAcademicYearUuidAndStatusOrderByEnrollmentCourseDepartmentNameAscGraduationNameAsc(academicYearUuid, GraduationCandidateStatus.APPROVED_FOR_GRADUATION);
        if (candidates.isEmpty()) throw notFound("No candidates are approved for graduation in this academic year");
        return finalBookletPdf.generate(year.getCode(), candidates);
    }

    @Transactional
    public GraduationCandidateDto approve(User registrar, Long id) {
        var a = graduationCandidateRepository.findDetailedByIdForUpdate(id).orElseThrow(() -> notFound("Graduation candidate not found"));
        if (a.getGraduationList().getSubmittedAt() == null)
            throw conflict("The department has not submitted this list");
        if (a.getClearanceStage() != GraduationClearanceStage.REGISTRAR_REVIEW)
            throw conflict("Candidate has not reached Registrar review");
        var clearance = clearanceApplications.findByEnrollmentIdAndAcademicYearId(a.getEnrollment().getId(), a.getGraduationList().getAcademicYear().getId()).orElseThrow(() -> conflict("Clearance application was not found"));
        if (clearanceChecks.existsByApplicationIdAndStageAndMandatoryTrueAndStatusNot(clearance.getId(), com.owuor.educue.clearance.enums.ClearanceDepartmentStage.GENERAL, com.owuor.educue.clearance.enums.ClearanceCheckStatus.CLEARED))
            throw conflict("Mandatory departmental clearance is incomplete");
        if (!clearanceChecks.existsByApplicationIdAndStage(clearance.getId(), com.owuor.educue.clearance.enums.ClearanceDepartmentStage.FINANCE) ||
            clearanceChecks.existsByApplicationIdAndStageAndMandatoryTrueAndStatusNot(clearance.getId(), com.owuor.educue.clearance.enums.ClearanceDepartmentStage.FINANCE, com.owuor.educue.clearance.enums.ClearanceCheckStatus.CLEARED))
            throw conflict("Finance clearance is incomplete");
        if (clearance.getStatus() != com.owuor.educue.clearance.enums.ClearanceApplicationStatus.CLEARED)
            throw conflict("Overall clearance is incomplete");
        if (a.getDetailsConfirmedAt() == null) throw conflict("The student has not confirmed graduation details");
        if (!Boolean.TRUE.equals(a.getClearanceComplete()) || !Boolean.TRUE.equals(a.getFinanceCleared()))
            throw conflict("Graduation clearance, including Finance, is incomplete");
        if (!graduationFeeCharged(a)) throw conflict("Graduation fee was not charged");
        if (!feePaid(a)) throw conflict("Graduation fee or student account balance is not settled");
        a.setStatus(GraduationCandidateStatus.APPROVED_FOR_GRADUATION);
        a.setClearanceStage(GraduationClearanceStage.COMPLETE);
        a.setAcademicApprovalBy(registrar);
        a.setAcademicApprovedAt(LocalDateTime.now());
        return response(graduationCandidateRepository.save(a));
    }

    private GraduationCandidateDto transientEntry(Enrollment e, com.owuor.educue.institution.entity.AcademicYear y, String remarks) {
        var a = new GraduationCandidate();
        var previewList = new GraduationList();
        previewList.setAcademicYear(y);
        previewList.setDepartment(e.getCourse().getDepartment());
        a.setGraduationList(previewList);
        a.setEnrollment(e);
        a.setAdmissionNumberSnapshot(e.getStudent().getAdmissionNumber());
        a.setCourseCodeSnapshot(e.getCourse().getCode());
        a.setCourseNameSnapshot(e.getCourse().getName());
        a.setGraduationName(e.getStudent().getFullName());
        a.setAwardTitle(e.getCourse().getAwardTitle());
        a.setQualificationType(e.getCourse().getQualificationType());
        a.setHodRemarks(remarks);
        applyAcademicSnapshot(a, e);
        return response(a);
    }

    /**
     * Builds a detached HOD preview without changing the stored graduation snapshot.
     */
    private GraduationCandidateDto liveAssessment(GraduationCandidate stored, Enrollment enrollment) {
        var preview = new GraduationCandidate();
        preview.setId(stored.getId());
        preview.setEnrollment(enrollment);
        preview.setGraduationList(stored.getGraduationList());
        preview.setAdmissionNumberSnapshot(stored.getAdmissionNumberSnapshot());
        preview.setCourseCodeSnapshot(stored.getCourseCodeSnapshot());
        preview.setCourseNameSnapshot(stored.getCourseNameSnapshot());
        preview.setGraduationName(stored.getGraduationName());
        preview.setAwardTitle(stored.getAwardTitle());
        preview.setQualificationType(stored.getQualificationType());
        preview.setHodRemarks(stored.getHodRemarks());
        preview.setStatus(stored.getStatus());
        preview.setDetailsConfirmedAt(stored.getDetailsConfirmedAt());
        preview.setClearanceComplete(stored.getClearanceComplete());
        preview.setFinanceCleared(stored.getFinanceCleared());
        applyAcademicSnapshot(preview, enrollment);
        return response(preview);
    }

    private GraduationCandidateDto basicEntry(Enrollment enrollment, com.owuor.educue.institution.entity.AcademicYear academicYear) {
        GraduationCandidate candidate = new GraduationCandidate();
        var previewList = new GraduationList();
        previewList.setAcademicYear(academicYear);
        previewList.setDepartment(enrollment.getCourse().getDepartment());
        candidate.setGraduationList(previewList);
        candidate.setEnrollment(enrollment);
        candidate.setAdmissionNumberSnapshot(enrollment.getStudent().getAdmissionNumber());
        candidate.setCourseCodeSnapshot(enrollment.getCourse().getCode());
        candidate.setCourseNameSnapshot(enrollment.getCourse().getName());
        candidate.setGraduationName(enrollment.getStudent().getFullName());
        candidate.setAwardTitle(enrollment.getCourse().getAwardTitle());
        candidate.setQualificationType(enrollment.getCourse().getQualificationType());
        return response(candidate);
    }

    private void applyAcademicSnapshot(GraduationCandidate candidate, Enrollment enrollment) {
        GraduationReadinessResponse assessment = readiness.assess(enrollment);
        GraduationReadiness graduationReadiness = new GraduationReadiness();
        graduationReadiness.setCandidate(candidate);
        graduationReadiness.setRequiredUnits(assessment.requiredUnits());
        graduationReadiness.setPassedUnits(assessment.passedUnits());
        graduationReadiness.setFailedUnits(assessment.failedUnits());
        graduationReadiness.setMissingResults(assessment.missingResults());
        graduationReadiness.setMissingUnits(assessment.missingUnits());
        graduationReadiness.setRequiredCredits(assessment.requiredCredits());
        graduationReadiness.setEarnedCredits(assessment.earnedCredits());
        graduationReadiness.setEligible(assessment.eligible());
        graduationReadiness.setAssessedAt(assessment.assessedAt());
        candidate.setReadiness(graduationReadiness);

        BigDecimal cumulativeAverage = resultService.calculateFinalCumulativeAverage(enrollment.getId());
        candidate.setFinalCumulativeAverage(cumulativeAverage);
        candidate.setAwardClassification(classify(cumulativeAverage, enrollment));
    }

    private AwardClassification classify(BigDecimal cumulativeAverage, Enrollment enrollment) {
        if (cumulativeAverage == null) return null;
        double averagePercentage = cumulativeAverage.doubleValue();
        return switch (enrollment.getCourse().getQualificationType()) {
            case BACHELOR ->
                    averagePercentage >= 70 ? AwardClassification.FIRST_CLASS_HONOURS : averagePercentage >= 60 ? AwardClassification.SECOND_CLASS_HONOURS_UPPER_DIVISION : averagePercentage >= 50 ? AwardClassification.SECOND_CLASS_HONOURS_LOWER_DIVISION : AwardClassification.PASS;
            case MASTERS ->
                    averagePercentage >= 70 ? AwardClassification.DISTINCTION : averagePercentage >= 60 ? AwardClassification.MERIT : AwardClassification.PASS;
            case CERTIFICATE, DIPLOMA ->
                    averagePercentage >= 70 ? AwardClassification.DISTINCTION : averagePercentage >= 60 ? AwardClassification.CREDIT : AwardClassification.PASS;
            case PHD -> AwardClassification.PASS;
        };
    }

    private GraduationCandidateDto response(GraduationCandidate candidate) {
        Enrollment enrollment = candidate.getEnrollment();
        var academicYear = candidate.getGraduationList().getAcademicYear();
        boolean eligible = academicEligible(candidate);
        GraduationReadiness assessment = candidate.getReadiness();
        List<Check> checks = assessment == null ? List.of() : readinessChecks(candidate, enrollment, assessment);
        BigDecimal outstandingBalance = candidate.getId() == null ? BigDecimal.ZERO : Optional.ofNullable(ledgers.getOutstandingBalance(enrollment.getStudent().getId())).orElse(BigDecimal.ZERO);
        return new GraduationCandidateDto(enrollment.getUuid(), enrollment.getStudent().getId(), enrollment.getStudent().getFullName(), enrollment.getStudent().getAdmissionNumber(), enrollment.getCourse().getCode(), enrollment.getCourse().getName(), enrollment.getCurrentCourseAcademicPeriod().getAcademicPeriod().getCode(), enrollment.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName(), isFinal(enrollment), assessment != null, eligible, candidate.getId(), academicYear.getUuid(), academicYear.getCode(), candidate.getGraduationList().getDepartment().getName(), candidate.getGraduationName(), candidate.getAwardTitle(), candidate.getFinalCumulativeAverage(), candidate.getAwardClassification() == null ? null : candidate.getAwardClassification().getDisplayName(), candidate.getHodRemarks(), candidate.getStatus() == null ? "NOT_STARTED" : candidate.getStatus().name(), candidate.getClearanceStage().name(), candidate.getDetailsConfirmedAt() != null, graduationFee(candidate), graduationFeeCharged(candidate), feePaid(candidate), Boolean.TRUE.equals(candidate.getClearanceComplete()), Boolean.TRUE.equals(candidate.getFinanceCleared()), outstandingBalance, checks, academicResults(candidate), clearanceItems(candidate), candidate.isCertificatePrinted(), candidate.getCertificatePrintedAt(), candidate.getCertificateNumber());
    }

    private List<Check> readinessChecks(GraduationCandidate candidate, Enrollment enrollment, GraduationReadiness assessment) {
        return List.of(
                new Check("FINAL_STAGE", "Final stage", isFinal(enrollment), isFinal(enrollment) ? "Final stage" : "Not final", "Final stage", null),
                new Check("UNITS_PASSED", "Units passed", nz(assessment.getFailedUnits()) == 0 && nz(assessment.getMissingResults()) == 0, nz(assessment.getPassedUnits()) + " / " + nz(assessment.getRequiredUnits()), String.valueOf(nz(assessment.getRequiredUnits())), null),
                new Check("UNITS_COMPLETE", "No missing units", nz(assessment.getMissingUnits()) == 0, String.valueOf(nz(assessment.getMissingUnits())), "0", null),
                new Check("RESULTS_COMPLETE", "No missing results", nz(assessment.getMissingResults()) == 0, String.valueOf(nz(assessment.getMissingResults())), "0", null),
                new Check("CUMULATIVE", "Cumulative result", candidate.getFinalCumulativeAverage() != null && candidate.getAwardClassification() != null, candidate.getFinalCumulativeAverage() == null ? "Not available" : candidate.getFinalCumulativeAverage() + "%", null, candidate.getFinalCumulativeAverage() == null ? "No eligible released results are available for cumulative calculation" : null),
                new Check("CREDITS", "Required credits", assessment.getRequiredCredits() != null && nz(assessment.getEarnedCredits()) >= assessment.getRequiredCredits(), String.valueOf(nz(assessment.getEarnedCredits())), String.valueOf(assessment.getRequiredCredits()), null));
    }

    private boolean academicEligible(GraduationCandidate candidate) {
        GraduationReadiness assessment = candidate.getReadiness();
        return assessment != null && isFinal(candidate.getEnrollment()) && assessment.isEligible()
                && candidate.getFinalCumulativeAverage() != null && candidate.getAwardClassification() != null;
    }

    private boolean feePaid(GraduationCandidate a) {
        return graduationFeeCharged(a) && Optional.ofNullable(ledgers.getOutstandingBalance(a.getEnrollment().getStudent().getId())).orElse(BigDecimal.ZERO).signum() <= 0;
    }

    private BigDecimal graduationFee(GraduationCandidate candidate) {
        return graduationFees.effectiveRules(candidate.getQualificationType(), candidate.getEnrollment().getIntake())
                .stream().map(rule -> rule.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean graduationFeeCharged(GraduationCandidate candidate) {
        return candidate.getId() != null && ledgers.existsByStudentIdAndTransactionType(
                candidate.getEnrollment().getStudent().getId(),
                com.owuor.educue.finance.enums.TransactionType.GRADUATION_FEE);
    }

    private boolean isFinal(Enrollment e) {
        return e.getCurrentCourseAcademicPeriod() != null && e.getCurrentCourseAcademicPeriod().getNextPeriod() == null;
    }

    private int nz(Integer x) {
        return x == null ? 0 : x;
    }

    private GraduationList list(UUID u, User h) {
        var y = year(u);
        return graduationListRepository.findByAcademicYearIdAndDepartmentId(y.getId(), department(h)).orElseGet(() -> {
            var l = new GraduationList();
            l.setAcademicYear(y);
            l.setDepartment(h.getDepartment());
            return graduationListRepository.save(l);
        });
    }

    private com.owuor.educue.institution.entity.AcademicYear year(UUID u) {
        return years.findByUuid(u).orElseThrow(() -> notFound("Academic year not found"));
    }

    private Long department(User h) {
        return h.getDepartment().getId();
    }

    private Specification<Enrollment> departmentScope(Long departmentId) {
        return (root, query, builder) -> builder.equal(root.get("department").get("id"), departmentId);
    }

    private boolean ownedByDepartment(Enrollment enrollment, Long departmentId) {
        return enrollment.getDepartment().getId().equals(departmentId);
    }

    private Enrollment owned(User h, UUID y, UUID id) {
        var e = enrollments.findByUuid(id).orElseThrow(() -> notFound("Enrollment not found"));
        if (!ownedByDepartment(e, department(h)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment is outside your department or academic year");
//        if (e.getStatus() != EnrollmentStatus.ACTIVE) throw conflict("Only active enrolments can be reviewed");
        return e;
    }

    private Summary summary(GraduationList l) {
        long count = graduationCandidateRepository.count((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId()));
        return new Summary(l.getUuid(), l.getAcademicYear().getUuid(), l.getAcademicYear().getCode(), l.getDepartment().getName(), l.getStatus(), count, count, l.getPublishedAt(), l.getSubmittedAt());
    }

    private Summary finalSummary(GraduationList graduationList) {
        long approvedCandidates = graduationCandidateRepository.count((root, query, builder) -> builder.and(
                builder.equal(root.get("graduationList").get("id"), graduationList.getId()),
                builder.equal(root.get("status"), GraduationCandidateStatus.APPROVED_FOR_GRADUATION)));
        return new Summary(graduationList.getUuid(), graduationList.getAcademicYear().getUuid(), graduationList.getAcademicYear().getCode(), graduationList.getDepartment().getName(), "FINAL", approvedCandidates, approvedCandidates, graduationList.getPublishedAt(), graduationList.getSubmittedAt());
    }

    private CandidateRow candidateRow(GraduationCandidate candidate) {
        return new CandidateRow(candidate.getId(), candidate.getGraduationName(), candidate.getAdmissionNumberSnapshot(),
                candidate.getCourseCodeSnapshot(), candidate.getCourseNameSnapshot(),
                Optional.ofNullable(ledgers.getOutstandingBalance(candidate.getEnrollment().getStudent().getId())).orElse(BigDecimal.ZERO),
                candidate.getClearanceStage().name(), Boolean.TRUE.equals(candidate.getFinanceCleared()),
                Boolean.TRUE.equals(candidate.getClearanceComplete()), candidate.getStatus().name(),
                candidate.isCertificatePrinted(), candidate.getCertificatePrintedAt());
    }

    private String require(String x, String m) {
        if (x == null || x.isBlank()) throw conflict(m);
        return x.trim();
    }

    private String blank(String x) {
        return x == null || x.isBlank() ? null : x.trim();
    }

    private ResponseStatusException conflict(String x) {
        return new ResponseStatusException(HttpStatus.CONFLICT, x);
    }

    private ResponseStatusException notFound(String x) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, x);
    }
}
