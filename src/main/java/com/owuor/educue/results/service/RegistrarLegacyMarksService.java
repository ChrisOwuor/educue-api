package com.owuor.educue.results.service;

import com.owuor.educue.academics.enums.RegistrationOrigin;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.results.dto.MarksEntryRowResponse;
import com.owuor.educue.results.dto.LegacyUnitGroupResponse;
import com.owuor.educue.results.dto.SaveMarksRequest;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrarLegacyMarksService {

    private final StudentUnitRegistrationRepository registrationRepository;
    private final StudentResultRepository resultRepository;
    private final GradingService gradingService;

    public List<LegacyUnitGroupResponse> getLegacyUnits() {
        var registrations = registrationRepository
                .findByRegistrationOriginOrderByCourseUnitPlacementUnitCodeAsc(
                         RegistrationOrigin.LEGACY);

        var byUnit = new LinkedHashMap<UUID, List<StudentUnitRegistration>>();
        registrations.forEach(registration -> byUnit
                .computeIfAbsent(registration.getCourseUnitPlacement().getUnit().getUuid(), ignored -> new ArrayList<>())
                .add(registration));

        return byUnit.values().stream().map(unitRegistrations -> {
            var first = unitRegistrations.getFirst();
            var unit = first.getCourseUnitPlacement().getUnit();
            var byPlacement = new LinkedHashMap<Long, List<StudentUnitRegistration>>();
            unitRegistrations.forEach(registration -> byPlacement
                    .computeIfAbsent(registration.getCourseUnitPlacement().getId(), ignored -> new ArrayList<>())
                    .add(registration));
            var placements = byPlacement.values().stream().map(placementRegistrations -> {
                var placement = placementRegistrations.getFirst().getCourseUnitPlacement();
                var course = placement.getCourseAcademicPeriod().getCourse();
                var period = placement.getCourseAcademicPeriod().getAcademicPeriod();
                return new LegacyUnitGroupResponse.Placement(
                        placement.getId(), placement.getUuid(), course.getId(), course.getUuid(),
                        course.getCode(), course.getName(), period.getCode(), period.getName(),
                        placementRegistrations.size());
            }).toList();
            return new LegacyUnitGroupResponse(
                    unit.getId(), unit.getUuid(), unit.getCode(), unit.getName(),
                    unitRegistrations.size(), placements);
        }).toList();
    }

    public List<MarksEntryRowResponse> getResultSheet(UUID unitUuid) {
        return registrationRepository
                .findByCourseUnitPlacementUnitUuidAndStatusAndRegistrationOriginOrderByEnrollmentStudentFullNameAsc(
                        unitUuid,
                        RegistrationStatus.ACTIVE,
                        RegistrationOrigin.LEGACY)
                .stream()
                .map(registration -> toResponse(
                        registration,
                        resultRepository.findByStudentUnitRegistrationId(registration.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public MarksEntryRowResponse saveMarks(SaveMarksRequest request, User registrar) {
        StudentUnitRegistration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new EntityNotFoundException("Registration not found"));

        if (registration.getRegistrationOrigin() != RegistrationOrigin.LEGACY) {
            throw new IllegalArgumentException("Registrar legacy marks can only be recorded against legacy registrations.");
        }
        if (registration.getStatus() != RegistrationStatus.ACTIVE) {
            throw new IllegalArgumentException("Marks can only be recorded against an active legacy registration.");
        }

        StudentResult result = resultRepository.findByStudentUnitRegistrationId(registration.getId())
                .orElseGet(() -> {
                    StudentResult created = new StudentResult();
                    created.setStudentUnitRegistration(registration);
                    return created;
                });

        result.setCaMarks(request.getCaMarks());
        result.setExamMarks(request.getExamMarks());
        result.setRemarks(request.getRemarks());
        result.setRecordedBy(registrar);
        gradingService.calculate(result);

        return toResponse(registration, resultRepository.save(result));
    }

    private MarksEntryRowResponse toResponse(StudentUnitRegistration registration, StudentResult result) {
        return MarksEntryRowResponse.builder()
                .registrationId(registration.getId())
                .studentId(registration.getEnrollment().getStudent().getId())
                .admissionNumber(registration.getEnrollment().getStudent().getAdmissionNumber())
                .studentName(registration.getEnrollment().getStudent().getFullName())
                .courseUnitPlacementId(registration.getCourseUnitPlacement().getId())
                .unitCode(registration.getCourseUnitPlacement().getUnit().getCode())
                .unitName(registration.getCourseUnitPlacement().getUnit().getName())
                .courseId(registration.getEnrollment().getCourse().getId())
                .courseCode(registration.getEnrollment().getCourse().getCode())
                .courseName(registration.getEnrollment().getCourse().getName())
                .attemptType(registration.getAttemptType().name())
                .resultId(result == null ? null : result.getId())
                .caMarks(result == null ? null : result.getCaMarks())
                .examMarks(result == null ? null : result.getExamMarks())
                .totalMarks(result == null ? null : result.getTotalMarks())
                .grade(result == null ? null : result.getGrade())
                .status(result == null ? null : result.getStatus().name())
                .build();
    }
}
