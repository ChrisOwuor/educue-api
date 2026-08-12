package com.owuor.educue.students.service;

import com.owuor.educue.common.dto.ApiPageResponse;
import com.owuor.educue.students.dto.EnrollmentFilterRequest;
import com.owuor.educue.students.dto.EnrollmentResponse;
import com.owuor.educue.students.dto.EnrollmentOverviewResponse;
import com.owuor.educue.students.dto.EnrollmentDetailResponse;
import com.owuor.educue.students.enums.EnrollmentStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
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

    @Transactional(readOnly = true)
    public ApiPageResponse<EnrollmentOverviewResponse> getEnrollmentOverview(EnrollmentFilterRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), overviewSort(request.getSort()));
        EnrollmentStatus status = request.getStatus() == null || request.getStatus().isBlank()
                ? null : EnrollmentStatus.valueOf(request.getStatus().trim().toUpperCase());
        String search = request.getSearch() == null || request.getSearch().isBlank()
                ? "" : request.getSearch().trim().toLowerCase();
        Page<EnrollmentOverviewResponse> page = enrollmentRepository.findOverview(search, status, pageable);
        return ApiPageResponse.<EnrollmentOverviewResponse>builder()
                .content(page.getContent()).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .first(page.isFirst()).last(page.isLast()).build();
    }

    @Transactional(readOnly = true)
    public EnrollmentDetailResponse getEnrollment(UUID uuid) {
        Enrollment e = enrollmentRepository.findDetailedByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found"));
        var department = e.getDepartment() != null ? e.getDepartment() : e.getCourse().getDepartment();
        return new EnrollmentDetailResponse(e.getUuid(), e.getStatus().name(), e.getAdmissionDate(),
                e.getStudent().getId(), e.getStudent().getAdmissionNumber(), e.getStudent().getFullName(),
                e.getStudent().getEmail(), e.getStudent().getPhone(), e.getCourse().getId(),
                e.getCourse().getCode(), e.getCourse().getName(), e.getIntake().getId(), e.getIntake().getName(),
                e.getEnrolledAcademicYear().getUuid(), e.getEnrolledAcademicYear().getCode(),
                e.getCurrentAcademicYear().getUuid(), e.getCurrentAcademicYear().getCode(),
                e.getCurrentCourseAcademicPeriod().getId(),
                e.getCurrentCourseAcademicPeriod().getAcademicPeriod().getCode(),
                e.getCurrentCourseAcademicPeriod().getAcademicPeriod().getName(),
                department.getId(), department.getName());
    }

    @Transactional
    public EnrollmentDetailResponse defer(UUID uuid) {
        Enrollment enrollment = enrollmentRepository.findDetailedByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE)
            throw new IllegalStateException("Only an active enrollment can be deferred");
        enrollment.setStatus(EnrollmentStatus.DEFERRED);
        enrollmentRepository.save(enrollment);
        return getEnrollment(uuid);
    }

    @Transactional(readOnly = true)
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

    private Sort overviewSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by("id").descending();
        String[] parts = sort.split(",");
        String property = switch (parts[0]) {
            case "status" -> "status";
            case "admissionDate" -> "admissionDate";
            default -> "id";
        };
        return Sort.by(parts.length == 2 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC, property);
    }
}
