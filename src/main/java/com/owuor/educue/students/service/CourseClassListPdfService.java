package com.owuor.educue.students.service;

import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.common.report.ProfessionalPdfService;
import com.owuor.educue.students.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Produces the official class list from active enrollments, not unit registrations. */
@Service
@RequiredArgsConstructor
public class CourseClassListPdfService {
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ProfessionalPdfService pdfService;

    @Transactional(readOnly = true)
    public byte[] generate(UUID courseUuid, UUID courseAcademicPeriodUuid) {
        var period = courseAcademicPeriodRepository.findByUuid(courseAcademicPeriodUuid)
                .orElseThrow(() -> new EntityNotFoundException("Course academic period not found"));

        if (!period.getCourse().getUuid().equals(courseUuid)) {
            throw new IllegalArgumentException("The selected academic period does not belong to this course");
        }

        var enrollments = enrollmentRepository.findActiveClassList(courseUuid, courseAcademicPeriodUuid);
        var details = new LinkedHashMap<String, String>();
        details.put("Course", period.getCourse().getCode() + " - " + period.getCourse().getName());
        details.put("Academic period", period.getAcademicPeriod().getCode() + " - " + period.getAcademicPeriod().getName());
        details.put("Active students", String.valueOf(enrollments.size()));

        var number = new AtomicInteger(1);
        List<List<String>> rows = enrollments.stream().map(enrollment -> List.of(
                String.valueOf(number.getAndIncrement()),
                enrollment.getStudent().getAdmissionNumber(),
                enrollment.getStudent().getFullName(),
                enrollment.getIntakeCourse().getIntake().getName()
        )).toList();

        return pdfService.tableReport("Class List", details,
                List.of("No.", "Admission number", "Student name", "Intake"), rows);
    }
}
