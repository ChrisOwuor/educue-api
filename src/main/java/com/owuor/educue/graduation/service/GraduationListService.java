package com.owuor.educue.graduation.service;

import com.owuor.educue.graduation.dto.GraduationListDtos.*;
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
    private final GraduationApplicationRepository entries;
    private final GraduationListRepository lists;
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
    public Page<Candidate> candidates(User hod, UUID yearUuid, String search, UUID academicPeriodUuid, boolean finalYearOnly, Pageable pageable) {
        var y = year(yearUuid);
        var department = department(hod);
        var selectedList = lists.findByAcademicYearIdAndDepartmentId(y.getId(), department).orElse(null);
        Specification<Enrollment> s = (r, q, b) -> b.equal(r.get("course").get("department").get("id"), department);
        s = s.and((r, q, b) -> b.notEqual(r.get("status"), EnrollmentStatus.GRADUATED));
        s = s.and((r, q, b) -> {
            var graduated = q.subquery(Long.class);
            var application = graduated.from(GraduationApplication.class);
            graduated.select(b.literal(1L));
            graduated.where(
                    b.equal(application.get("enrollment").get("id"), r.get("id")),
                    application.get("status").in(GraduationApplicationStatus.GRADUATED, GraduationApplicationStatus.CONFERRED)
            );
            return b.not(b.exists(graduated));
        });
        if (search != null && !search.isBlank()) {
            String v = "%" + search.trim().toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("student").get("fullName")), v), b.like(b.lower(r.get("student").get("admissionNumber")), v)));
        }
        if (academicPeriodUuid != null)
            s = s.and((r, q, b) -> b.equal(r.get("currentCourseAcademicPeriod").get("academicPeriod").get("uuid"), academicPeriodUuid));
        if (finalYearOnly) s = s.and((r, q, b) -> b.isNull(r.get("currentCourseAcademicPeriod").get("nextPeriod")));
        return enrollments.findAll(s, pageable).map(e -> {
            var a = selectedList == null ? null : entries.findByGraduationListIdAndEnrollmentId(selectedList.getId(), e.getId()).orElse(null);
            return new Candidate(e.getUuid(), e.getStudent().getId(), e.getStudent().getFullName(), e.getStudent().getAdmissionNumber(), e.getCourse().getCode(), e.getCourse().getName(), e.getCurrentCourseAcademicPeriod().getAcademicPeriod().getCode(), e.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName(), isFinal(e), a != null, a != null && academicEligible(a), a == null ? null : a.getId(), a == null ? null : a.getStatus().name());
        });
    }

    @Transactional
    public Entry assess(User hod, UUID yearUuid, UUID enrollmentId) {
        var e = owned(hod, yearUuid, enrollmentId);
        var selectedList = lists.findByAcademicYearIdAndDepartmentId(year(yearUuid).getId(), department(hod)).orElse(null);
        var existing = selectedList == null ? null : entries.findByGraduationListIdAndEnrollmentId(selectedList.getId(), e.getId()).orElse(null);
        if (existing != null) return response(existing);
        return transientEntry(e, year(yearUuid), null);
    }

    @Transactional
    public Entry enrollmentDetail(User hod, UUID enrollmentId) {
        var e = enrollments.findByUuid(enrollmentId).orElseThrow(() -> notFound("Enrollment not found"));
        if (!e.getCourse().getDepartment().getId().equals(department(hod))) throw notFound("Enrollment not found");
        var yearUuid = e.getCurrentAcademicYear().getUuid();
        var selectedList = lists.findByAcademicYearIdAndDepartmentId(year(yearUuid).getId(), department(hod)).orElse(null);
        var existing = selectedList == null ? null : entries.findByGraduationListIdAndEnrollmentId(selectedList.getId(), e.getId()).orElse(null);
        if (existing != null) return isFinal(e) ? liveAssessment(existing, e) : response(existing);
        return isFinal(e) ? transientEntry(e, year(yearUuid), null) : basicEntry(e, year(yearUuid));
    }

    @Transactional
    public Entry add(User hod, AddRequest req) {
        var e = owned(hod, req.academicYearUuid(), req.enrollmentUuid());
        if (e.getStatus() == EnrollmentStatus.GRADUATED || entries.existsByEnrollmentIdAndStatusIn(e.getId(), List.of(GraduationApplicationStatus.GRADUATED, GraduationApplicationStatus.CONFERRED)))
            throw conflict("This student has already graduated and cannot be added to another graduation list");
        if (entries.findByGraduationListIdAndEnrollmentId(list(req.academicYearUuid(), hod).getId(), e.getId()).isPresent())
            throw conflict("Student is already on a graduation list");
        Entry assessment = transientEntry(e, year(req.academicYearUuid()), req.remarks());
        if (!assessment.eligible()) throw conflict("The student is not academically eligible for graduation");
        var list = lists.findByAcademicYearIdAndDepartmentId(year(req.academicYearUuid()).getId(), department(hod)).orElseThrow(() -> conflict("Create the departmental graduation list before adding candidates"));
        if (!"DRAFT".equals(list.getStatus())) throw conflict("Only a draft graduation list can be changed");
        var a = new GraduationApplication();
        a.setGraduationList(list);
        a.setEnrollment(e);
        a.setStatus(GraduationApplicationStatus.DRAFT);
        a.setAdmissionNumberSnapshot(e.getStudent().getAdmissionNumber());
        a.setCourseCodeSnapshot(e.getCourse().getCode());
        a.setCourseNameSnapshot(e.getCourse().getName());
        a.setGraduationName(e.getStudent().getFullName());
        a.setAwardTitle(require(e.getCourse().getAwardTitle(), "Award title is not configured"));
        a.setQualificationType(e.getCourse().getQualificationType());
        a.setHodRemarks(blank(req.remarks()));
        applyAcademicSnapshot(a, e);
        var rules = graduationFees.effectiveRules(e.getCourse().getQualificationType(), e.getIntake());
        if (rules.isEmpty()) throw conflict("No graduation fee is configured for this qualification and intake");
        BigDecimal total = rules.stream().map(x -> x.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        a.setTotalAmount(total);
        for (var rule : rules) {
            var item = new GraduationApplicationFeeItem();
            item.setFeeItemUuid(rule.getFeeItem().getUuid());
            item.setFeeItemCode(rule.getFeeItem().getCode());
            item.setFeeItemName(rule.getFeeItem().getName());
            item.setAmount(rule.getAmount());
            item.setDisplayOrder(rule.getDisplayOrder());
            a.addFeeItem(item);
        }
        return response(entries.save(a));
    }

    @Transactional
    public Summary publish(User hod, UUID yearUuid) {
        var l = list(yearUuid, hod);
        if (!"DRAFT".equals(l.getStatus())) throw conflict("Only a draft list can be published");
        if (entries.count((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId())) == 0)
            throw conflict("Add at least one eligible candidate first");
        l.setStatus("PROVISIONAL");
        l.setPublishedBy(hod);
        l.setPublishedAt(LocalDateTime.now());
        entries.findAll((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId())).forEach(a -> a.setStatus(GraduationApplicationStatus.PROVISIONAL));
        return summary(lists.save(l));
    }

    @Transactional
    public Summary unpublish(User hod, UUID yearUuid) {
        var list = list(yearUuid, hod);
        if (!"PROVISIONAL".equals(list.getStatus()))
            throw conflict("Only a provisional graduation list can be unpublished");
        var candidates = entries.findAll((r, q, b) -> b.equal(r.get("graduationList").get("id"), list.getId()));
        boolean studentActionStarted = candidates.stream().anyMatch(candidate ->
                candidate.getDetailsConfirmedAt() != null ||
                candidate.getLedgerEntry() != null ||
                candidate.getStatus() != GraduationApplicationStatus.PROVISIONAL);
        if (studentActionStarted)
            throw conflict("This list cannot be unpublished because a candidate has already confirmed details or started graduation processing");
        candidates.forEach(candidate -> candidate.setStatus(GraduationApplicationStatus.DRAFT));
        list.setStatus("DRAFT");
        list.setPublishedBy(null);
        list.setPublishedAt(null);
        return summary(lists.save(list));
    }

    @Transactional
    public Summary submit(User hod, UUID yearUuid) {
        var l = list(yearUuid, hod);
        if (!"PROVISIONAL".equals(l.getStatus())) throw conflict("Publish the provisional list before submitting it");
        l.setStatus("SUBMITTED");
        l.setSubmittedBy(hod);
        l.setSubmittedAt(LocalDateTime.now());
        return summary(lists.save(l));
    }

    @Transactional(readOnly = true)
    public Summary hodList(User hod, UUID yearUuid) {
        var y = year(yearUuid);
        return lists.findByAcademicYearIdAndDepartmentId(y.getId(), department(hod)).map(this::summary).orElse(new Summary(null, y.getUuid(), y.getCode(), hod.getDepartment().getName(), "DRAFT", 0, 0, null, null));
    }

    @Transactional(readOnly = true)
    public Entry mine(User user) {
        var e = enrollments.findByStudentUserId(user.getId()).orElseThrow(() -> notFound("Student enrollment not found"));
        var a = entries.findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(e.getId(), List.of(GraduationApplicationStatus.DRAFT, GraduationApplicationStatus.REMOVED)).orElseThrow(() -> notFound("Student is not on a published graduation list"));
        if (a.getStatus() == GraduationApplicationStatus.DRAFT)
            throw notFound("Student is not on a published graduation list");
        return response(a);
    }

    @Transactional
    public Entry updateDetails(User user, UpdateDetailsRequest request) {
        var e = enrollments.findByStudentUserIdForUpdate(user.getId()).orElseThrow(() -> notFound("Student enrollment not found"));
        var a = entries.findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(e.getId(), List.of(GraduationApplicationStatus.DRAFT, GraduationApplicationStatus.REMOVED)).orElseThrow(() -> notFound("Student is not on a published graduation list"));
        if (a.getDetailsConfirmedAt() != null) throw conflict("Graduation details are already confirmed");
        String name = require(request.graduationName(), "Graduation name is required");
        if (name.length() > 180) throw conflict("Graduation name cannot exceed 180 characters");
        a.setGraduationName(name);
        return response(entries.save(a));
    }

    @Transactional
    public Entry confirm(User user) {
        var e = enrollments.findByStudentUserIdForUpdate(user.getId()).orElseThrow(() -> notFound("Student enrollment not found"));
        var a = entries.findFirstByEnrollmentIdAndStatusNotInOrderByGraduationListAcademicYearStartDateDesc(e.getId(), List.of(GraduationApplicationStatus.DRAFT, GraduationApplicationStatus.REMOVED)).orElseThrow(() -> notFound("Student is not on a published graduation list"));
        if (a.getDetailsConfirmedAt() != null) return response(a);
        if (a.getStatus() != GraduationApplicationStatus.PROVISIONAL && a.getStatus() != GraduationApplicationStatus.SUBMITTED_TO_REGISTRAR)
            throw conflict("Graduation details are not open for confirmation");
        if (a.getLedgerEntry() == null) a.setLedgerEntry(ledgerService.billGraduation(e, a.getTotalAmount()));
        a.setDetailsConfirmedAt(LocalDateTime.now());
        a.setStatus(GraduationApplicationStatus.DETAILS_CONFIRMED);
        return response(entries.save(a));
    }

    @Transactional
    public Summary createList(User hod, UUID yearUuid) {
        var y = year(yearUuid);
        if (lists.findByAcademicYearIdAndDepartmentId(y.getId(), department(hod)).isPresent())
            throw conflict("A departmental graduation list already exists for this academic year");
        var l = new GraduationList();
        l.setAcademicYear(y);
        l.setDepartment(hod.getDepartment());
        return summary(lists.save(l));
    }

    @Transactional
    public Summary updateList(User hod, UUID listId, UUID yearUuid) {
        var list = ownedList(hod, listId);
        if (!"DRAFT".equals(list.getStatus()))
            throw conflict("Only a draft graduation list can be edited");
        var targetYear = year(yearUuid);
        lists.findByAcademicYearIdAndDepartmentId(targetYear.getId(), department(hod))
                .filter(existing -> !existing.getId().equals(list.getId()))
                .ifPresent(existing -> { throw conflict("A departmental graduation list already exists for this academic year"); });
        list.setAcademicYear(targetYear);
        return summary(lists.save(list));
    }

    @Transactional
    public void deleteList(User hod, UUID listId) {
        var list = ownedList(hod, listId);
        if (!"DRAFT".equals(list.getStatus()))
            throw conflict("Only a draft graduation list can be deleted");
        long candidates = entries.count((r, q, b) -> b.equal(r.get("graduationList").get("id"), list.getId()));
        if (candidates > 0)
            throw conflict("Remove all candidates before deleting this graduation list");
        lists.delete(list);
    }

    @Transactional(readOnly = true)
    public Page<Summary> hodLists(User hod, Pageable pageable) {
        Specification<GraduationList> s = (r, q, b) -> b.equal(r.get("department").get("id"), department(hod));
        return lists.findAll(s, pageable).map(this::summary);
    }

    @Transactional(readOnly = true)
    public Page<Entry> hodListEntries(User hod, UUID listId, String search, Pageable pageable) {
        var l = ownedList(hod, listId);
        Specification<GraduationApplication> s = (r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId());
        if (search != null && !search.isBlank()) {
            String v = "%" + search.trim().toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("graduationName")), v), b.like(b.lower(r.get("admissionNumberSnapshot")), v)));
        }
        return entries.findAll(s, pageable).map(this::response);
    }

    @Transactional
    public void removeListEntry(User hod, UUID listId, UUID enrollmentUuid) {
        var list = ownedList(hod, listId);
        if (!"DRAFT".equals(list.getStatus()))
            throw conflict("Candidates can only be removed from a draft graduation list");
        var enrollment = enrollments.findByUuid(enrollmentUuid)
                .orElseThrow(() -> notFound("Candidate was not found on this graduation list"));
        var candidate = entries.findByGraduationListIdAndEnrollmentId(list.getId(), enrollment.getId())
                .orElseThrow(() -> notFound("Candidate was not found on this graduation list"));
        if (candidate.getDetailsConfirmedAt() != null || candidate.getLedgerEntry() != null)
            throw conflict("A candidate with confirmed details or graduation charges cannot be removed");
        entries.delete(candidate);
    }

    @Transactional(readOnly = true)
    public byte[] hodListPdf(User hod, UUID listId) {
        var l = ownedList(hod, listId);
        var rows = entries.findAll((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId()), Sort.by("graduationName"));
        return graduationListPdfRenderer.render(l, rows);
    }

    private GraduationList ownedList(User hod, UUID id) {
        var l = lists.findByUuid(id).orElseThrow(() -> notFound("Graduation list not found"));
        if (!l.getDepartment().getId().equals(department(hod)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Graduation list belongs to another department");
        return l;
    }

    @Transactional(readOnly = true)
    public Page<Summary> staffLists(User user, UUID yearUuid, Long departmentId, Pageable pageable) {
        Specification<GraduationList> s = (r, q, b) -> b.equal(r.get("status"), "SUBMITTED");
        if (yearUuid != null) s = s.and((r, q, b) -> b.equal(r.get("academicYear").get("uuid"), yearUuid));
        if (departmentId != null) s = s.and((r, q, b) -> b.equal(r.get("department").get("id"), departmentId));
        return lists.findAll(s, pageable).map(this::summary);
    }

    @Transactional(readOnly = true)
    public Page<Entry> staffListEntries(User user, UUID listId, String search, Pageable pageable) {
        var selectedList = lists.findByUuid(listId).orElseThrow(() -> notFound("Graduation list not found"));
        Specification<GraduationApplication> s = (r, q, b) -> b.equal(r.get("graduationList").get("id"), selectedList.getId());
        s = staffStage(user, s);
        if (search != null && !search.isBlank()) {
            String v = "%" + search.trim().toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("graduationName")), v), b.like(b.lower(r.get("admissionNumberSnapshot")), v)));
        }
        return entries.findAll(s, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<CandidateRow> staffListCandidates(User user, UUID listId, String search, Pageable pageable) {
        var selectedList = lists.findByUuid(listId).orElseThrow(() -> notFound("Graduation list not found"));
        Specification<GraduationApplication> s = (r, q, b) -> b.equal(r.get("graduationList").get("id"), selectedList.getId());
        s = staffStage(user, s);
        if (search != null && !search.isBlank()) {
            String value = "%" + search.trim().toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("graduationName")), value),
                    b.like(b.lower(r.get("admissionNumberSnapshot")), value)));
        }
        return entries.findAll(s, pageable).map(a -> new CandidateRow(a.getId(), a.getGraduationName(),
                a.getAdmissionNumberSnapshot(), a.getCourseCodeSnapshot(), a.getAwardTitle(), a.getStatus().name()));
    }

    private List<AcademicResult> academicResults(GraduationApplication a) {
        return resultRepository.findByStudentUnitRegistrationEnrollmentId(a.getEnrollment().getId()).stream().sorted(Comparator.comparing(x -> x.getStudentUnitRegistration().getCourseUnitPlacement().getCourseAcademicPeriod().getPosition())).map(r -> {
            var p = r.getStudentUnitRegistration().getCourseUnitPlacement();
            var period = p.getCourseAcademicPeriod().getAcademicPeriod();
            var unit = p.getUnit();
            return new AcademicResult(r.getId(), period.getCode(), period.getName(), unit.getCode(), unit.getName(), unit.getCreditHours(), r.getStudentUnitRegistration().getAttemptType().name(), r.getCaMarks(), r.getExamMarks(), r.getTotalMarks(), r.getGrade(), r.isPassed(), r.getStatus().name());
        }).toList();
    }

    private List<ClearanceItem> clearanceItems(GraduationApplication a) {
        return clearanceApplications.findByEnrollmentIdAndAcademicYearId(a.getEnrollment().getId(), a.getGraduationList().getAcademicYear().getId()).map(ca -> ca.getChecks().stream().map(c -> new ClearanceItem(c.getId(), c.getDepartment().getName(), c.getStatus().name(), c.getRemarks(), c.getReviewedBy() == null ? null : c.getReviewedBy().getFullName(), c.getReviewedAt(), c.getStage() == com.owuor.educue.clearance.enums.ClearanceDepartmentStage.FINANCE)).toList()).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Page<Entry> registrarEntries(String search, String status, UUID yearUuid, Pageable pageable) {
        Specification<GraduationApplication> s = (r, q, b) -> b.equal(r.get("graduationList").get("status"), "SUBMITTED");
        if (yearUuid != null)
            s = s.and((r, q, b) -> b.equal(r.get("graduationList").get("academicYear").get("uuid"), yearUuid));
        if (status != null && !status.isBlank()) {
            var x = GraduationApplicationStatus.valueOf(status.toUpperCase());
            s = s.and((r, q, b) -> b.equal(r.get("status"), x));
        } else s = s.and((r, q, b) -> b.equal(r.get("clearanceStage"), GraduationClearanceStage.REGISTRAR_REVIEW));
        if (search != null && !search.isBlank()) {
            String v = "%" + search.toLowerCase() + "%";
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("admissionNumberSnapshot")), v), b.like(b.lower(r.get("enrollment").get("student").get("fullName")), v)));
        }
        return entries.findAll(s, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public Entry registrarEntry(Long id) {
        var candidate = entries.findDetailedById(id).orElseThrow(() -> notFound("Graduation candidate not found"));
        if (candidate.getClearanceStage() != GraduationClearanceStage.REGISTRAR_REVIEW && candidate.getStatus() != GraduationApplicationStatus.APPROVED_FOR_GRADUATION)
            throw notFound("Candidate has not reached Registrar review");
        return response(candidate);
    }

    @Transactional(readOnly = true)
    public Entry staffEntry(User user, Long id) {
        var candidate = entries.findDetailedById(id).orElseThrow(() -> notFound("Graduation candidate not found"));
        String role = user.getRole().getName();
        if ("FINANCE".equals(role) && candidate.getClearanceStage() != GraduationClearanceStage.FINANCE_REVIEW)
            throw notFound("Candidate has not reached Finance review");
        if ("REGISTRAR".equals(role) && candidate.getClearanceStage() != GraduationClearanceStage.REGISTRAR_REVIEW && candidate.getStatus() != GraduationApplicationStatus.APPROVED_FOR_GRADUATION)
            throw notFound("Candidate has not reached Registrar review");
        return response(candidate);
    }

    private Specification<GraduationApplication> staffStage(User user, Specification<GraduationApplication> specification) {
        String role = user.getRole().getName();
        if ("FINANCE".equals(role)) return specification.and((r, q, b) -> b.equal(r.get("clearanceStage"), GraduationClearanceStage.FINANCE_REVIEW));
        if ("REGISTRAR".equals(role)) return specification.and((r, q, b) ->
                b.equal(r.get("clearanceStage"), GraduationClearanceStage.REGISTRAR_REVIEW));
        return specification;
    }

    @Transactional(readOnly = true)
    public byte[] certificate(Long id) {
        var a = entries.findDetailedById(id).orElseThrow(() -> notFound("Graduation candidate not found"));
        if (a.getStatus() != GraduationApplicationStatus.APPROVED_FOR_GRADUATION)
            throw conflict("Only approved-for-graduation candidates can receive certificates");
        var e = a.getEnrollment();
        var c = new GraduationCertificate();
        c.setStudentName(a.getGraduationName());
        c.setAdmissionNumber(a.getAdmissionNumberSnapshot());
        c.setCourseCode(a.getCourseCodeSnapshot());
        c.setCourseName(a.getCourseNameSnapshot());
        c.setAwardTitle(a.getAwardTitle());
        c.setAwardClassification(a.getAwardClassification() == null ? null : a.getAwardClassification().getDisplayName());
        c.setGraduationDate(a.getGraduationList().getAcademicYear().getEndDate());
        c.setCertificateNumber("GRAD-" + a.getGraduationList().getAcademicYear().getStartYear() + "-" + String.format("%06d", a.getId()));
        return certificateRenderer.render(c);
    }

    @Transactional(readOnly = true)
    public byte[] finalBooklet(UUID academicYearUuid) {
        var year = years.findByUuid(academicYearUuid).orElseThrow(() -> notFound("Academic year not found"));
        var candidates = entries.findByGraduationListAcademicYearUuidAndStatusOrderByEnrollmentCourseDepartmentNameAscGraduationNameAsc(academicYearUuid, GraduationApplicationStatus.APPROVED_FOR_GRADUATION);
        if (candidates.isEmpty()) throw notFound("No candidates are approved for graduation in this academic year");
        return finalBookletPdf.generate(year.getCode(), candidates);
    }

    @Transactional
    public Entry approve(User registrar, Long id) {
        var a = entries.findDetailedByIdForUpdate(id).orElseThrow(() -> notFound("Graduation candidate not found"));
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
        if (a.getLedgerEntry() == null) throw conflict("Graduation fee was not charged");
        if (!feePaid(a)) throw conflict("Graduation fee or student account balance is not settled");
        a.setStatus(GraduationApplicationStatus.APPROVED_FOR_GRADUATION);
        a.setClearanceStage(GraduationClearanceStage.COMPLETE);
        a.setAcademicApprovalBy(registrar);
        a.setAcademicApprovedAt(LocalDateTime.now());
        return response(entries.save(a));
    }

    private Entry transientEntry(Enrollment e, com.owuor.educue.institution.entity.AcademicYear y, String remarks) {
        var a = new GraduationApplication();
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
        a.setTotalAmount(BigDecimal.ZERO);
        a.setHodRemarks(remarks);
        applyAcademicSnapshot(a, e);
        return response(a);
    }

    /** Builds a detached HOD preview without changing the stored graduation snapshot. */
    private Entry liveAssessment(GraduationApplication stored, Enrollment enrollment) {
        var preview = new GraduationApplication();
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
        preview.setTotalAmount(stored.getTotalAmount());
        preview.setLedgerEntry(stored.getLedgerEntry());
        preview.setClearanceComplete(stored.getClearanceComplete());
        preview.setFinanceCleared(stored.getFinanceCleared());
        applyAcademicSnapshot(preview, enrollment);
        return response(preview);
    }

    private Entry basicEntry(Enrollment e, com.owuor.educue.institution.entity.AcademicYear y) {
        var a = new GraduationApplication();
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
        a.setTotalAmount(BigDecimal.ZERO);
        return response(a);
    }

    private void applyAcademicSnapshot(GraduationApplication a, Enrollment e) {
        var r = readiness.assess(e);
        a.setRequiredUnits(r.requiredUnits());
        a.setPassedUnits(r.passedUnits());
        a.setFailedUnits(r.failedUnits());
        a.setMissingResults(r.missingResults());
        a.setMissingUnits(r.missingUnits());
        a.setRequiredCredits(r.requiredCredits());
        a.setEarnedCredits(r.earnedCredits());
        a.setEligibilityAssessedAt(r.assessedAt());
        BigDecimal avg = resultService.calculateFinalCumulativeAverage(e.getId());
        a.setFinalCumulativeAverage(avg);
        a.setAwardClassification(classify(avg, e));
    }

    private AwardClassification classify(BigDecimal x, Enrollment e) {
        if (x == null) return null;
        double v = x.doubleValue();
        return switch (e.getCourse().getQualificationType()) {
            case BACHELOR ->
                    v >= 70 ? AwardClassification.FIRST_CLASS_HONOURS : v >= 60 ? AwardClassification.SECOND_CLASS_HONOURS_UPPER_DIVISION : v >= 50 ? AwardClassification.SECOND_CLASS_HONOURS_LOWER_DIVISION : AwardClassification.PASS;
            case MASTERS ->
                    v >= 70 ? AwardClassification.DISTINCTION : v >= 60 ? AwardClassification.MERIT : AwardClassification.PASS;
            case CERTIFICATE, DIPLOMA ->
                    v >= 70 ? AwardClassification.DISTINCTION : v >= 60 ? AwardClassification.CREDIT : AwardClassification.PASS;
            case PHD -> AwardClassification.PASS;
        };
    }

    private Entry response(GraduationApplication a) {
        var e = a.getEnrollment();
        var y = a.getGraduationList().getAcademicYear();
        boolean eligible = academicEligible(a);
        var checks = List.of(new Check("FINAL_STAGE", "Final stage", isFinal(e), isFinal(e) ? "Final stage" : "Not final", "Final stage", null), new Check("UNITS_PASSED", "Units passed", nz(a.getFailedUnits()) == 0 && nz(a.getMissingResults()) == 0, nz(a.getPassedUnits()) + " / " + nz(a.getRequiredUnits()), String.valueOf(nz(a.getRequiredUnits())), null), new Check("UNITS_COMPLETE", "No missing units", nz(a.getMissingUnits()) == 0, String.valueOf(nz(a.getMissingUnits())), "0", null), new Check("RESULTS_COMPLETE", "No missing results", nz(a.getMissingResults()) == 0, String.valueOf(nz(a.getMissingResults())), "0", null), new Check("CUMULATIVE", "Cumulative result", a.getFinalCumulativeAverage() != null && a.getAwardClassification() != null, a.getFinalCumulativeAverage() == null ? "Not available" : a.getFinalCumulativeAverage() + "%", null, a.getFinalCumulativeAverage() == null ? "No eligible released results are available for cumulative calculation" : null), new Check("CREDITS", "Required credits", a.getRequiredCredits() != null && nz(a.getEarnedCredits()) >= a.getRequiredCredits(), nz(a.getEarnedCredits()) + "", String.valueOf(a.getRequiredCredits()), null));
        BigDecimal balance = a.getId() == null ? BigDecimal.ZERO : Optional.ofNullable(ledgers.getOutstandingBalance(e.getStudent().getId())).orElse(BigDecimal.ZERO);
        return new Entry(e.getUuid(), e.getStudent().getId(), e.getStudent().getFullName(), e.getStudent().getAdmissionNumber(), e.getCourse().getCode(), e.getCourse().getName(), e.getCurrentCourseAcademicPeriod().getAcademicPeriod().getCode(), e.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName(), isFinal(e), a.getEligibilityAssessedAt() != null, eligible, a.getId(), y.getUuid(), y.getCode(), a.getGraduationList().getDepartment().getName(), a.getGraduationName(), a.getAwardTitle(), a.getFinalCumulativeAverage(), a.getAwardClassification() == null ? null : a.getAwardClassification().getDisplayName(), a.getHodRemarks(), a.getStatus() == null ? "DRAFT" : a.getStatus().name(), a.getClearanceStage().name(), a.getDetailsConfirmedAt() != null, a.getTotalAmount(), a.getLedgerEntry() != null, feePaid(a), Boolean.TRUE.equals(a.getClearanceComplete()), Boolean.TRUE.equals(a.getFinanceCleared()), balance, checks, academicResults(a), clearanceItems(a));
    }

    private boolean academicEligible(GraduationApplication a) {
        return isFinal(a.getEnrollment()) && nz(a.getFailedUnits()) == 0 && nz(a.getMissingResults()) == 0 && nz(a.getMissingUnits()) == 0 && a.getRequiredCredits() != null && nz(a.getEarnedCredits()) >= a.getRequiredCredits() && a.getFinalCumulativeAverage() != null && a.getAwardClassification() != null;
    }

    private boolean feePaid(GraduationApplication a) {
        return a.getLedgerEntry() != null && Optional.ofNullable(ledgers.getOutstandingBalance(a.getEnrollment().getStudent().getId())).orElse(BigDecimal.ZERO).signum() <= 0;
    }

    private boolean isFinal(Enrollment e) {
        return e.getCurrentCourseAcademicPeriod() != null && e.getCurrentCourseAcademicPeriod().getNextPeriod() == null;
    }

    private int nz(Integer x) {
        return x == null ? 0 : x;
    }

    private GraduationList list(UUID u, User h) {
        var y = year(u);
        return lists.findByAcademicYearIdAndDepartmentId(y.getId(), department(h)).orElseGet(() -> {
            var l = new GraduationList();
            l.setAcademicYear(y);
            l.setDepartment(h.getDepartment());
            return lists.save(l);
        });
    }

    private com.owuor.educue.institution.entity.AcademicYear year(UUID u) {
        return years.findByUuid(u).orElseThrow(() -> notFound("Academic year not found"));
    }

    private Long department(User h) {
        if (h.getDepartment() == null) throw conflict("HOD is not assigned to a department");
        return h.getDepartment().getId();
    }

    private Enrollment owned(User h, UUID y, UUID id) {
        var e = enrollments.findByUuid(id).orElseThrow(() -> notFound("Enrollment not found"));
        if (!e.getCourse().getDepartment().getId().equals(department(h)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enrollment is outside your department or academic year");
//        if (e.getStatus() != EnrollmentStatus.ACTIVE) throw conflict("Only active enrolments can be reviewed");
        return e;
    }

    private Summary summary(GraduationList l) {
        long count = entries.count((r, q, b) -> b.equal(r.get("graduationList").get("id"), l.getId()));
        return new Summary(l.getUuid(), l.getAcademicYear().getUuid(), l.getAcademicYear().getCode(), l.getDepartment().getName(), l.getStatus(), count, count, l.getPublishedAt(), l.getSubmittedAt());
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
