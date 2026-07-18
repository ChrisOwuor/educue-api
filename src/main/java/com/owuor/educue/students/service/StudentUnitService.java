package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.students.dto.StudentUnitResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentUnitService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseUnitPlacementRepository placementRepository;

    public List<StudentUnitResponse> getCurrentPeriodUnits(Long userId) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Active enrollment not found"));
        int intakeYear = enrollment.getIntakeCourse().getIntake().getStartDate().getYear();
        return placementRepository
                .findActiveForPeriodAndIntakeYear(enrollment.getCurrentCourseAcademicPeriod().getId(), intakeYear)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private StudentUnitResponse toResponse(
            CourseUnitPlacement item
    ) {
        return StudentUnitResponse.builder()
                .courseUnitPlacementId(item.getId())
                .courseUnitPlacementUuid(item.getUuid())
                .courseAcademicPeriodUuid(item.getCourseAcademicPeriod().getUuid())
                .unitId(item.getUnit().getId())
                .unitCode(item.getUnit().getCode())
                .unitName(item.getUnit().getName())
                .creditHours(item.getUnit().getCreditHours())
                .unitType(item.getUnitType())
                .academicPeriodCode(item.getCourseAcademicPeriod().getAcademicPeriod().getCode())
                .academicPeriodName(item.getCourseAcademicPeriod().getAcademicPeriod().getName())
                .build();
    }
}
