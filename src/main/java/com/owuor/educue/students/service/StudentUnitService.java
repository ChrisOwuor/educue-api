package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.students.dto.StudentUnitResponse;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
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

    public List<StudentUnitResponse> getCurrentPeriodUnits(
            Long userId
    ) {
        Enrollment enrollment =
                enrollmentRepository
                        .findByStudentUserId(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Active enrollment not found"
                                )
                        );

        Long coursePeriodId =
                enrollment
                        .getCurrentCourseAcademicPeriod()
                        .getId();

        Long intakeSequence =
                enrollment
                        .getIntake()
                        .getSequenceNumber();

        return placementRepository
                .findEffectiveForPeriodAndIntakeSequence(
                        coursePeriodId,
                        intakeSequence
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }



    private StudentUnitResponse toResponse(
            CourseUnitPlacement placement
    ) {
        return StudentUnitResponse.builder()
                .courseUnitPlacementId(
                        placement.getId()
                )
                .courseUnitPlacementUuid(
                        placement.getUuid()
                )
                .courseAcademicPeriodUuid(
                        placement
                                .getCourseAcademicPeriod()
                                .getUuid()
                )
                .unitId(
                        placement.getUnit().getId()
                )
                .unitCode(
                        placement.getUnit().getCode()
                )
                .unitName(
                        placement.getUnit().getName()
                )
                .creditHours(
                        placement
                                .getUnit()
                                .getCreditHours()
                )
                .unitType(
                        placement.getUnitType()
                )
                .academicPeriodCode(
                        placement
                                .getCourseAcademicPeriod()
                                .getAcademicPeriod()
                                .getCode()
                )
                .academicPeriodName(
                        placement
                                .getCourseAcademicPeriod()
                                .getAcademicPeriod()
                                .getName()
                )
                .build();
    }


    public List<StudentUnitResponse> getUnitsUpToCurrentPeriod(
            Long userId
    ) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Active enrollment not found"
                        )
                );

        var currentCoursePeriod =
                enrollment.getCurrentCourseAcademicPeriod();

        Long courseId = currentCoursePeriod
                .getCourse()
                .getId();

        Integer currentPosition =
                currentCoursePeriod.getPosition();

        Long intakeSequence = enrollment
                .getIntake()
                .getSequenceNumber();

        return placementRepository
                .findEffectiveHistoryForStudent(
                        enrollment.getId(),
                        courseId,
                        currentPosition,
                        intakeSequence,
                        RegistrationStatus.ACTIVE
                )
                .stream()
                .map(row ->
                        toUnitsResponse(
                                row.placement(),
                                row.registration()
                        )
                )
                .toList();
    }
    private StudentUnitResponse toUnitsResponse(
            CourseUnitPlacement placement,
            StudentUnitRegistration registration
    ) {
        return StudentUnitResponse.builder()
                .registrationId(
                        registration == null
                                ? null
                                : registration.getId()
                )
                .courseUnitPlacementId(
                        placement.getId()
                )
                .courseUnitPlacementUuid(
                        placement.getUuid()
                )
                .courseAcademicPeriodUuid(
                        placement
                                .getCourseAcademicPeriod()
                                .getUuid()
                )
                .unitId(
                        placement.getUnit().getId()
                )
                .unitCode(
                        placement.getUnit().getCode()
                )
                .unitName(
                        placement.getUnit().getName()
                )
                .creditHours(
                        placement.getUnit().getCreditHours()
                )
                .unitType(
                        placement.getUnitType()
                )
                .academicPeriodCode(
                        placement
                                .getCourseAcademicPeriod()
                                .getAcademicPeriod()
                                .getCode()
                )
                .academicPeriodName(
                        placement
                                .getCourseAcademicPeriod()
                                .getAcademicPeriod()
                                .getName()
                )
                .attemptType(
                        registration == null
                                ? null
                                : String.valueOf(registration.getAttemptType())
                )
                .registrationOrigin(
                        registration == null
                                ? null
                                : String.valueOf(registration.getRegistrationOrigin())
                )
                .build();
    }
}
