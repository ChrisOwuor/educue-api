package com.owuor.educue.students.service;

import com.owuor.educue.common.dto.ApiPageResponse;
import com.owuor.educue.students.dto.EnrollmentFilterRequest;
import com.owuor.educue.students.dto.EnrollmentResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.EnrollmentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public ApiPageResponse<EnrollmentResponse> getEnrollments(
            EnrollmentFilterRequest request
    ) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                buildSort(request.getSort())
        );

        Specification<Enrollment> specification =
                Specification.where(
                                EnrollmentSpecification.search(
                                        request.getSearch()
                                )
                        )
                        .and(
                                EnrollmentSpecification.course(
                                        request.getCourseId()
                                )
                        )
                        .and(
                                EnrollmentSpecification.courseAcademicPeriod(
                                        request.getCourseAcademicPeriodId()
                                )
                        )
                        .and(
                                EnrollmentSpecification.status(
                                        request.getStatus()
                                )
                        );

        Page<Enrollment> page =
                enrollmentRepository.findAll(
                        specification,
                        pageable
                );

        List<EnrollmentResponse> content =
                page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ApiPageResponse.<EnrollmentResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private EnrollmentResponse toResponse(
            Enrollment enrollment
    ) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentUserId(enrollment.getStudent().getUser().getId())
                .admissionNumber(
                        enrollment.getStudent()
                                .getAdmissionNumber()
                )
                .courseAcademicPeriodId(enrollment.getCurrentCourseAcademicPeriod().getId())
                .studentName(
                        enrollment.getStudent()
                                .getFullName()
                )
                .email(
                        enrollment.getStudent()
                                .getEmail()
                )
                .courseName(
                        enrollment.getCourse()
                                .getName()
                )
                .courseId(enrollment.getCourse().getId())
                .intakeId(enrollment.getIntake().getId())
                .intakeName(enrollment.getIntake().getName())
                .enrolledAcademicYearUuid(enrollment.getEnrolledAcademicYear().getUuid())
                .enrolledAcademicYearCode(enrollment.getEnrolledAcademicYear().getCode())
                .currentAcademicYearUuid(enrollment.getCurrentAcademicYear().getUuid())
                .currentAcademicYearCode(enrollment.getCurrentAcademicYear().getCode())
                .academicPeriodName(
                        enrollment.getCurrentCourseAcademicPeriod()
                                .getAcademicPeriod().getName()
                )
                .status(
                        enrollment.getStatus().name()
                )
                .build();
    }

    private Sort buildSort(String sort) {

        if (sort == null || sort.isBlank()) {
            return Sort.by("id").descending();
        }

        String[] parts = sort.split(",");
        String property = switch (parts[0]) {
            case "admissionNumber" -> "student.admissionNumber";
            case "studentName" -> "student.fullName";
            case "course" -> "course.name";
            case "academicPeriod" -> "currentCourseAcademicPeriod.academicPeriod.name";
            case "status" -> "status";
            case "id" -> "id";
            default -> throw new IllegalArgumentException("Unsupported enrollment sort field: " + parts[0]);
        };
        Sort.Direction direction = parts.length == 2 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
