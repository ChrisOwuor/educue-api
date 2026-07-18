package com.owuor.educue.results.service;

import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.results.dto.BatchApprovalRequest;
import com.owuor.educue.results.dto.StudentResultFilterRequest;
import com.owuor.educue.results.dto.StudentResultResponse;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.results.repository.StudentResultSpecification;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.users.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class StudentResultService {

    private final StudentResultRepository resultRepository;

    public Page<StudentResultResponse> getResults(StudentResultFilterRequest filter, Pageable pageable) {
        return resultRepository.findAll(
                StudentResultSpecification.withFilters(
                        filter.getSearch(),
                        filter.getCourseId(),
                        filter.getCourseAcademicPeriodId(),
                        filter.getCourseUnitPlacementId(),
                        filter.getStatus(),
                        filter.getPassed()
                ),
                pageable
        ).map(this::toResponse);
    }

    private StudentResultResponse toResponse(StudentResult result) {
        StudentUnitRegistration registration = result.getStudentUnitRegistration();
        Enrollment enrollment = registration.getEnrollment();
        var placement = registration.getCourseUnitPlacement();

        return StudentResultResponse.builder()
                .resultId(result.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getFullName())
                .admissionNumber(enrollment.getStudent().getAdmissionNumber())
                .unitCode(placement.getUnit().getCode())
                .unitName(placement.getUnit().getName())
                .course(enrollment.getIntakeCourse().getCourse().getName())
                .academicPeriod(placement.getCourseAcademicPeriod().getAcademicPeriod().getName())
                .attemptType(registration.getAttemptType().name())
                .caMarks(result.getCaMarks())
                .examMarks(result.getExamMarks())
                .totalMarks(result.getTotalMarks())
                .grade(result.getGrade())
                .passed(result.isPassed())
                .status(result.getStatus().name())
                .remarks(result.getRemarks())
                .recordedByName(result.getRecordedBy() != null ? result.getRecordedBy().getFullName() : null)
                .approvedByName(result.getApprovedBy() != null ? result.getApprovedBy().getFullName() : null)
                .approvedAt(result.getApprovedAt())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }


    public List<StudentResultResponse> getMyResults(Long userId) {

        return resultRepository
                .findByStudentUnitRegistrationEnrollmentStudentUserIdOrderByStudentUnitRegistrationCourseUnitPlacementCourseAcademicPeriodPosition(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public void updateBatchStatus(BatchApprovalRequest request, User hodUser) {
        if (request.resultIds() == null || request.resultIds().isEmpty()) {
            return;
        }

        // Retrieve all targeted results
        List<StudentResult> results = resultRepository.findAllById(request.resultIds());

        for (StudentResult result : results) {

            if (request.status() == ResultStatus.APPROVED) {
                result.approve(hodUser);
            } else {
                result.withhold("Results withheld you will be contacted by the hod ");
            }
        }

        // Persist modifications back into DB
        resultRepository.saveAll(results);
    }
}
