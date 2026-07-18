package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.academics.repository.LecturerUnitAssignmentRepository;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.students.dto.*;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentUnitRegistrationService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseUnitPlacementRepository placementRepository;
    private final StudentUnitRegistrationRepository registrationRepository;
    private final FeeLedgerService feeLedgerService;
    private final LecturerUnitAssignmentRepository lecturerAssignmentRepository;
    private final AcademicYearRepository academicYearRepository;

    public RegistrationResponse registerUnits(Long userId, List<Long> placementIds) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Active enrollment not found."));
        FeeStructure fees = enrollmentRepository.findCurrentFeeStructure(enrollment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Fee structure not found for the current academic period."));
        feeLedgerService.validateUnitRegistrationEligibility(enrollment.getStudent().getId(), fees.getId());

        List<CourseUnitPlacement> placements = placementRepository.findAllById(placementIds);
        if (placements.size() != placementIds.size()) throw new IllegalArgumentException("One or more selected units do not exist.");
        int intakeYear = enrollment.getIntakeCourse().getIntake().getStartDate().getYear();
        List<StudentUnitRegistration> registrations = new ArrayList<>();
        for (CourseUnitPlacement placement : placements) {
            boolean valid = placement.isActive()
                    && placement.getCourseAcademicPeriod().getId().equals(enrollment.getCurrentCourseAcademicPeriod().getId())
                    && placement.getEffectiveFromIntakeYear() <= intakeYear
                    && (placement.getEffectiveToIntakeYear() == null || placement.getEffectiveToIntakeYear() >= intakeYear);
            if (!valid) throw new IllegalArgumentException("Unit " + placement.getUnit().getCode() + " is not available in your current academic period.");
            if (registrationRepository.existsByEnrollmentIdAndCourseUnitPlacementIdAndStatus(
                    enrollment.getId(), placement.getId(), RegistrationStatus.ACTIVE)) continue;
            StudentUnitRegistration registration = new StudentUnitRegistration();
            registration.setEnrollment(enrollment);
            registration.setCourseUnitPlacement(placement);
            registration.setAttemptType(AttemptType.NORMAL);
            registration.setStatus(RegistrationStatus.ACTIVE);
            registrations.add(registration);
        }
        if (registrations.isEmpty()) return new RegistrationResponse("The selected units are already registered.", 0);
        registrationRepository.saveAll(registrations);
        return new RegistrationResponse(registrations.size() + " unit(s) registered successfully.", registrations.size());
    }

    @Transactional(readOnly = true)
    public List<StudentUnitResponse> getRegisteredUnits(Long userId) {
        return registrationRepository.findByEnrollmentStudentUserIdAndStatusOrderByCourseUnitPlacementUnitCode(
                userId, RegistrationStatus.ACTIVE).stream().map(this::toStudentUnit).toList();
    }

    @Transactional(readOnly = true)
    public RegisteredStudentsResponse getRegisteredStudents(Long placementId, User requester) {
        assertCanViewPlacement(placementId, requester);
        CourseUnitPlacement placement = placementRepository.findById(placementId)
                .orElseThrow(() -> new EntityNotFoundException("Course unit placement not found."));
        List<StudentUnitRegistration> registrations = registrationRepository
                .findByCourseUnitPlacementIdAndStatusOrderByEnrollmentStudentAdmissionNumberAsc(placementId, RegistrationStatus.ACTIVE);
        return RegisteredStudentsResponse.builder()
                .courseUnitPlacementId(placement.getId()).unitCode(placement.getUnit().getCode())
                .unitName(placement.getUnit().getName()).courseAcademicPeriodId(placement.getCourseAcademicPeriod().getId())
                .academicPeriodName(placement.getCourseAcademicPeriod().getAcademicPeriod().getName())
                .totalRegistered(registrations.size()).students(registrations.stream().map(this::toRegisteredStudent).toList()).build();
    }

    @Transactional(readOnly = true)
    public void assertCanViewPlacement(Long placementId, User requester) {
        String role = requester.getRole().getName();
        if ("ADMIN".equals(role) || "HOD".equals(role)) return;
        if (!"TRAINER".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot view this unit roster.");
        }
        var currentYear = academicYearRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException("No current academic year is configured."));
        boolean assigned = lecturerAssignmentRepository.findByLecturerIdOrderByAssignedAtDesc(requester.getId())
                .stream()
                .anyMatch(item -> item.getCourseUnitPlacement().getId().equals(placementId)
                        && item.isActiveFor(currentYear));
        if (!assigned) {
            throw new org.springframework.security.access.AccessDeniedException("This unit is not allocated to you in the current academic year.");
        }
    }

    @Transactional(readOnly = true)
    public List<StudentUnitRegistrationResponse> getAllRegistrations() {
        return registrationRepository.findByStatusOrderByRegisteredAtDesc(RegistrationStatus.ACTIVE)
                .stream().map(this::toRegistration).toList();
    }

    @Transactional(readOnly = true)
    public Page<StudentUnitRegistrationResponse> getRegistrations(StudentUnitRegistrationFilterRequest filter, Pageable pageable) {
        return registrationRepository.findAll(StudentUnitRegistrationSpecification.withFilters(
                filter.getSearch(), filter.getCourseId(), filter.getCourseAcademicPeriodId(),
                filter.getCourseUnitPlacementId(), filter.getAttemptType(), filter.getStatus()), pageable).map(this::toRegistration);
    }

    private StudentUnitResponse toStudentUnit(StudentUnitRegistration registration) {
        var placement = registration.getCourseUnitPlacement();
        var period = placement.getCourseAcademicPeriod().getAcademicPeriod();
        return StudentUnitResponse.builder().courseUnitPlacementId(placement.getId())
                .courseUnitPlacementUuid(placement.getUuid()).courseAcademicPeriodUuid(placement.getCourseAcademicPeriod().getUuid())
                .unitId(placement.getUnit().getId()).unitCode(placement.getUnit().getCode()).unitName(placement.getUnit().getName())
                .creditHours(placement.getUnit().getCreditHours()).unitType(placement.getUnitType())
                .academicPeriodCode(period.getCode()).academicPeriodName(period.getName()).build();
    }

    private RegisteredStudentResponse toRegisteredStudent(StudentUnitRegistration registration) {
        return RegisteredStudentResponse.builder().registrationId(registration.getId())
                .studentId(registration.getEnrollment().getStudent().getId())
                .admissionNumber(registration.getEnrollment().getStudent().getAdmissionNumber())
                .studentName(registration.getEnrollment().getStudent().getFullName())
                .attemptType(registration.getAttemptType().name()).status(registration.getStatus().name())
                .registeredAt(registration.getRegisteredAt()).build();
    }

    private StudentUnitRegistrationResponse toRegistration(StudentUnitRegistration registration) {
        var enrollment = registration.getEnrollment();
        var placement = registration.getCourseUnitPlacement();
        return StudentUnitRegistrationResponse.builder().registrationId(registration.getId())
                .studentId(enrollment.getStudent().getId()).courseUnitPlacementId(placement.getId())
                .studentName(enrollment.getStudent().getFullName())
                .admissionNumber(enrollment.getStudent().getAdmissionNumber())
                .course(enrollment.getIntakeCourse().getCourse().getName())
                .currentAcademicPeriod(enrollment.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName())
                .academicPeriod(placement.getCourseAcademicPeriod().getAcademicPeriod().getName())
                .unitCode(placement.getUnit().getCode()).unitName(placement.getUnit().getName())
                .attemptType(registration.getAttemptType().name()).status(registration.getStatus().name())
                .registeredAt(registration.getRegisteredAt()).build();
    }
}
