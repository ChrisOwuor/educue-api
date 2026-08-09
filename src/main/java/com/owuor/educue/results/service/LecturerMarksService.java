package com.owuor.educue.results.service;

import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.results.dto.MarksEntryRowResponse;
import com.owuor.educue.results.dto.SaveMarksRequest;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.academics.repository.LecturerUnitAssignmentRepository;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LecturerMarksService {

    private final StudentUnitRegistrationRepository registrationRepository;
    private final GradingService gradingService;

    private final StudentResultRepository resultRepository;
    private final LecturerUnitAssignmentRepository assignmentRepository;
    private final AcademicYearRepository academicYearRepository;

    public List<MarksEntryRowResponse> getResultSheet(Long courseUnitPlacementId) {

        List<StudentUnitRegistration> registrations =
                registrationRepository
                        .findByCourseUnitPlacementIdAndStatusOrderByEnrollmentStudentFullNameAsc(
                                courseUnitPlacementId,
                                RegistrationStatus.ACTIVE
                        );


        return registrations.stream()

                .map(registration -> {

                    StudentResult result = resultRepository
                            .findByStudentUnitRegistrationId(registration.getId())
                            .orElse(null);

                    return MarksEntryRowResponse.builder()

                            .registrationId(registration.getId())

                            .studentId(registration.getEnrollment().getStudent().getId())

                            .admissionNumber(
                                    registration.getEnrollment()
                                            .getStudent()
                                            .getAdmissionNumber()
                            )

                            .studentName(
                                    registration.getEnrollment()
                                            .getStudent()
                                            .getFullName()
                            )

                            .courseUnitPlacementId(
                                    registration.getCourseUnitPlacement().getId()
                            )

                            .unitCode(
                                    registration.getCourseUnitPlacement()
                                            .getUnit()
                                            .getCode()
                            )

                            .unitName(
                                    registration.getCourseUnitPlacement()
                                            .getUnit()
                                            .getName()
                            )

                            .courseId(registration.getEnrollment().getCourse().getId())
                            .courseCode(registration.getEnrollment().getCourse().getCode())
                            .courseName(registration.getEnrollment().getCourse().getName())

                            .attemptType(
                                    registration.getAttemptType().name()
                            )

                            .resultId(
                                    result != null ? result.getId() : null
                            )

                            .caMarks(
                                    result != null ? result.getCaMarks() : null
                            )

                            .examMarks(
                                    result != null ? result.getExamMarks() : null
                            )

                            .totalMarks(
                                    result != null ? result.getTotalMarks() : null
                            )

                            .grade(
                                    result != null ? result.getGrade() : null
                            )

                            .status(
                                    result != null ? result.getStatus().name() : null
                            )

                            .build();

                })

                .toList();
    }

    public List<MarksEntryRowResponse> getGroupedResultSheet(Long lecturerId, UUID unitUuid) {
        var currentYear = academicYearRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException("No current academic year is configured."));
        List<Long> placementIds = assignmentRepository.findByLecturerIdOrderByAssignedAtDesc(lecturerId).stream()
                .filter(assignment -> assignment.isActiveFor(currentYear))
                .map(assignment -> assignment.getCourseUnitPlacement())
                .filter(placement -> placement.getUnit().getUuid().equals(unitUuid))
                .map(placement -> placement.getId())
                .distinct()
                .toList();
        if (placementIds.isEmpty()) {
            throw new EntityNotFoundException("No active lecturer allocation was found for this unit.");
        }
        return registrationRepository
                .findByCourseUnitPlacementIdInAndStatusOrderByEnrollmentStudentFullNameAsc(
                        placementIds, RegistrationStatus.ACTIVE)
                .stream().map(registration -> {
                    StudentResult result = resultRepository.findByStudentUnitRegistrationId(registration.getId()).orElse(null);
                    return toResponse(registration, result);
                }).toList();
    }

    @Transactional
    public MarksEntryRowResponse saveMarks(
            SaveMarksRequest request,
            User lecturer
    ) {

        StudentUnitRegistration registration =
                registrationRepository.findById(request.getRegistrationId())
                        .orElseThrow(() ->
                                new EntityNotFoundException("Registration not found"));

        StudentResult result = resultRepository
                .findByStudentUnitRegistrationId(registration.getId())
                .orElseGet(() -> {

                    StudentResult r = new StudentResult();

                    r.setStudentUnitRegistration(registration);

                    return r;
                });

        result.setCaMarks(request.getCaMarks());

        result.setExamMarks(request.getExamMarks());

        result.setRemarks(request.getRemarks());

        result.setRecordedBy(lecturer);

        gradingService.calculate(result);


        return toResponse(registration,resultRepository.save(result));

    }

    private MarksEntryRowResponse toResponse(
            StudentUnitRegistration registration,
            StudentResult result
    ) {
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
