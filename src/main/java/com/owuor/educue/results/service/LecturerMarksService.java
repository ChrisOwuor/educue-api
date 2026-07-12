package com.owuor.educue.results.service;

import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.results.dto.MarksEntryRowResponse;
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

@Service
@RequiredArgsConstructor
public class LecturerMarksService {

    private final StudentUnitRegistrationRepository registrationRepository;
    private final GradingService gradingService;

    private final StudentResultRepository resultRepository;

    public List<MarksEntryRowResponse> getResultSheet(Long semesterUnitId) {

        List<StudentUnitRegistration> registrations =
                registrationRepository
                        .findBySemesterUnitIdAndStatusOrderByEnrollmentStudentFullNameAsc(
                                semesterUnitId,
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

                            .semesterUnitId(
                                    registration.getSemesterUnit().getId()
                            )

                            .unitCode(
                                    registration.getSemesterUnit()
                                            .getUnit()
                                            .getCode()
                            )

                            .unitName(
                                    registration.getSemesterUnit()
                                            .getUnit()
                                            .getName()
                            )

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
                .semesterUnitId(registration.getSemesterUnit().getId())
                .unitCode(registration.getSemesterUnit().getUnit().getCode())
                .unitName(registration.getSemesterUnit().getUnit().getName())
                .attemptType(registration.getAttemptType().name())
                .resultId(result.getId())
                .caMarks(result.getCaMarks())
                .examMarks(result.getExamMarks())
                .totalMarks(result.getTotalMarks())
                .grade(result.getGrade())
                .status(result.getStatus().name())
                .build();
    }
}
