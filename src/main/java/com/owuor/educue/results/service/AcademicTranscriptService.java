package com.owuor.educue.results.service;

import com.owuor.educue.common.report.ProfessionalPdfService;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class AcademicTranscriptService {

    private final StudentResultService resultService;
    private final EnrollmentRepository enrollmentRepository;
    private final ProfessionalPdfService pdfService;

    @Transactional
    public byte[] generateProvisionalTranscript(
            Long userId,
            Long courseAcademicPeriodId,
            String outcome
    ) {
        Enrollment enrollment =
                enrollmentRepository
                        .findByStudentUserId(
                                userId
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Active enrollment not found."
                                )
                        );

        var academicResults =
                resultService.getMyAcademicResults(
                        userId,
                        courseAcademicPeriodId,
                        outcome
                );

        LinkedHashMap<String, String> details =
                new LinkedHashMap<>();

        details.put(
                "Student Name",
                enrollment
                        .getStudent()
                        .getFullName()
        );

        details.put(
                "Admission Number",
                enrollment
                        .getStudent()
                        .getAdmissionNumber()
        );

        details.put(
                "Course",
                enrollment
                        .getCourse()
                        .getName()
        );

        details.put(
                "Course Code",
                enrollment
                        .getCourse()
                        .getCode()
        );

        details.put(
                "Current Stage",
                enrollment
                        .getCurrentCourseAcademicPeriod()
                        .getAcademicPeriod()
                        .getName()
        );

        details.put(
                "Document Status",
                "PROVISIONAL"
        );

        details.put(
                "Date Generated",
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "dd MMMM yyyy HH:mm"
                                )
                        )
        );

        details.put(
                "Document Reference",
                buildReference(enrollment)
        );

        return pdfService
                .provisionalAcademicTranscript(
                        details,
                        academicResults
                );
    }

    private String buildReference(
            Enrollment enrollment
    ) {
        String admissionNumber =
                enrollment
                        .getStudent()
                        .getAdmissionNumber()
                        .replaceAll(
                                "[^A-Za-z0-9]",
                                ""
                        );

        return "PROV-"
               + admissionNumber
               + "-"
               + LocalDateTime.now()
                       .format(
                               DateTimeFormatter.ofPattern(
                                       "yyyyMMddHHmmss"
                               )
                       );
    }
}
