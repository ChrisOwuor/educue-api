package com.owuor.educue.students.service;


import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.academics.repository.SemesterUnitRepository;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.students.dto.*;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationSpecification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentUnitRegistrationService {

    private final EnrollmentRepository enrollmentRepository;
    private final SemesterUnitRepository semesterUnitRepository;
    private final StudentUnitRegistrationRepository registrationRepository;
    private final FeeLedgerService feeLedgerService;

    @Transactional
    public RegistrationResponse registerUnits(Long userId, List<Long> semesterUnitIds) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(userId).orElseThrow(() -> new IllegalArgumentException("Active enrollment not found."));
//        feeLedgerService.validateUnitRegistrationEligibility(enrollment.getStudent().getId());

        FeeStructure feeStructure = enrollmentRepository
                .findCurrentFeeStructure(enrollment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Fee structure not found."));

        feeLedgerService.validateUnitRegistrationEligibility(enrollment.getStudent().getId(),feeStructure.getId());

        List<SemesterUnit> semesterUnits = semesterUnitRepository.findAllById(semesterUnitIds);
        if (semesterUnits.size() != semesterUnitIds.size()) {
            throw new IllegalArgumentException("One or more selected units do not exist.");
        }
        List<StudentUnitRegistration> registrations = new ArrayList<>();
        for (SemesterUnit semesterUnit : semesterUnits) {
            if (!semesterUnit.getSemester().getId().equals(enrollment.getCurrentSemester().getId())) {
                throw new IllegalArgumentException("Unit " + semesterUnit.getUnit().getCode() + " does not belong to your current semester.");
            }
            boolean alreadyRegistered = registrationRepository.existsByEnrollmentIdAndSemesterUnitIdAndStatus(enrollment.getId(), semesterUnit.getId(), RegistrationStatus.ACTIVE);
            if (alreadyRegistered) {
                continue;
            }
            StudentUnitRegistration registration = new StudentUnitRegistration();
            registration.setEnrollment(enrollment);
            registration.setSemesterUnit(semesterUnit);
            registration.setAttemptType(AttemptType.NORMAL);
            registration.setStatus(RegistrationStatus.ACTIVE);
            registrations.add(registration);
        }
        if (registrations.isEmpty()) {
            return new RegistrationResponse("No new units were registered. The selected units are already registered.", 0);
        }
        registrationRepository.saveAll(registrations);
        return new RegistrationResponse(registrations.size() + " unit(s) registered successfully.", registrations.size());
    }

    @Transactional()
    public List<StudentUnitResponse> getRegisteredUnits(Long userId) {

        return registrationRepository
                .findCurrentRegisteredUnitsByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional()
    public List<RegisteredStudentResponse> getRegisteredStudentsn(Long semesterUnitId) {

        return registrationRepository
                .findBySemesterUnitIdAndStatusOrderByEnrollmentStudentAdmissionNumberAsc(
                        semesterUnitId,
                        RegistrationStatus.ACTIVE
                )
                .stream()
                .map(this::toRegisteredStudentResponse)
                .toList();
    }

    private RegisteredStudentResponse toRegisteredStudentResponse(
            StudentUnitRegistration registration
    ) {

        return RegisteredStudentResponse.builder()
                .registrationId(registration.getId())
                .studentId(registration.getEnrollment().getStudent().getId())
                .admissionNumber(registration.getEnrollment().getStudent().getAdmissionNumber())
                .studentName(registration.getEnrollment().getStudent().getFullName())
                .attemptType(registration.getAttemptType().name())
                .status(registration.getStatus().name())
                .registeredAt(registration.getRegisteredAt())
                .build();
    }


    @Transactional()
    public RegisteredStudentsResponse getRegisteredStudents(Long semesterUnitId) {

        List<StudentUnitRegistration> registrations =
                registrationRepository.findBySemesterUnitIdAndStatus(
                        semesterUnitId,
                        RegistrationStatus.ACTIVE
                );

        if (registrations.isEmpty()) {
            throw new EntityNotFoundException("No registered students found.");
        }

        SemesterUnit semesterUnit =
                registrations.getFirst().getSemesterUnit();

        return RegisteredStudentsResponse.builder()
                .semesterUnitId(semesterUnit.getId())
                .unitCode(semesterUnit.getUnit().getCode())
                .unitName(semesterUnit.getUnit().getName())
                .semesterId(semesterUnit.getSemester().getId())
                .semesterName(semesterUnit.getSemester().getName())
                .totalRegistered(registrations.size())
                .students(
                        registrations.stream()
                                .map(this::toRegisteredStudentResponse)
                                .toList()
                )
                .build();
    }


    private StudentUnitResponse toResponse(StudentUnitRegistration registration) {

        SemesterUnit item = registration.getSemesterUnit();

        return StudentUnitResponse.builder()
                .semesterUnitId(item.getId())
                .semesterId(item.getSemester().getId())
                .unitId(item.getUnit().getId())
                .unitCode(item.getUnit().getCode())
                .unitName(item.getUnit().getName())
                .creditHours(item.getUnit().getCreditHours())
                .isMandatory(item.isMandatory())
                .category(item.getCategory())
                .semesterName(item.getSemester().getName())
                .build();
    }

    @Transactional()
    public List<StudentUnitRegistrationResponse> getAllRegistrations() {

        return registrationRepository
                .findByStatusOrderByRegisteredAtDesc(RegistrationStatus.ACTIVE)
                .stream()
                .map(this::toUnitResponse)
                .toList();
    }


    private StudentUnitRegistrationResponse toUnitResponse(
            StudentUnitRegistration registration
    ) {

        Enrollment enrollment = registration.getEnrollment();

        SemesterUnit semesterUnit = registration.getSemesterUnit();

        Semester semester = semesterUnit.getSemester();

        CourseCurriculum curriculum = enrollment.getCourseCurriculum();

        return StudentUnitRegistrationResponse.builder()
                .registrationId(registration.getId())
                .currentSemester(enrollment.getCurrentSemester().getName())

                .studentId(enrollment.getStudent().getId())

                .studentName(enrollment.getStudent().getFullName())

                .admissionNumber(
                        enrollment.getStudent().getAdmissionNumber()
                )

                .course(
                        curriculum.getCourse().getName()
                )

                .curriculum(
                        curriculum.getName()
                )

                .semester(
                        semester.getName()
                )

                .unitCode(
                        semesterUnit.getUnit().getCode()
                )

                .unitName(
                        semesterUnit.getUnit().getName()
                )

                .attemptType(
                        registration.getAttemptType().name()
                )

                .status(
                        registration.getStatus().name()
                )

                .registeredAt(
                        registration.getRegisteredAt()
                )

                .build();
    }


    public Page<StudentUnitRegistrationResponse> getRegistrations(

            StudentUnitRegistrationFilterRequest filter,

            Pageable pageable

    ) {

        return registrationRepository.findAll(

                        StudentUnitRegistrationSpecification.withFilters(

                                filter.getSearch(),

                                filter.getCourseId(),

                                filter.getCurriculumId(),

                                filter.getSemesterId(),

                                filter.getSemesterUnitId(),

                                filter.getAttemptType(),

                                filter.getStatus()

                        ),

                        pageable

                )

                .map(this::toUnitResponse2);

    }

    private StudentUnitRegistrationResponse toUnitResponse2(
            StudentUnitRegistration registration
    ) {

        Enrollment enrollment = registration.getEnrollment();

        SemesterUnit semesterUnit = registration.getSemesterUnit();

        Semester semester = semesterUnit.getSemester();

        CourseCurriculum curriculum = enrollment.getCourseCurriculum();

        return StudentUnitRegistrationResponse.builder()

                .registrationId(
                        registration.getId()
                )

                .currentSemester(
                        enrollment.getCurrentSemester().getName()
                )

                .studentId(
                        enrollment.getStudent().getId()
                )

                .studentName(
                        enrollment.getStudent().getFullName()
                )

                .admissionNumber(
                        enrollment.getStudent().getAdmissionNumber()
                )

                .course(
                        curriculum.getCourse().getName()
                )

                .curriculum(
                        curriculum.getName()
                )

                .semester(
                        semester.getName()
                )

                .unitCode(
                        semesterUnit.getUnit().getCode()
                )

                .unitName(
                        semesterUnit.getUnit().getName()
                )

                .attemptType(
                        registration.getAttemptType().name()
                )

                .status(
                        registration.getStatus().name()
                )

                .registeredAt(
                        registration.getRegisteredAt()
                )

                .build();

    }
}
