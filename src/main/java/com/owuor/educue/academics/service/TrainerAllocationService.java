package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.*;
import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.entity.TrainerAssignment;
import com.owuor.educue.academics.repository.CourseCurriculumRepository;
import com.owuor.educue.academics.repository.SemesterUnitRepository;
import com.owuor.educue.academics.repository.SemesterUnitSpecification;
import com.owuor.educue.academics.repository.TrainerAssignmentRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainerAllocationService {

    private final SemesterUnitRepository semesterUnitRepository;
    private final TrainerAssignmentRepository trainerAssignmentRepository;
    private final CourseCurriculumRepository courseCurriculumRepository;
    private final UserRepository userRepository;

    public Page<TrainerAllocationResponse> get(
            TrainerAllocationFilterRequest filter,
            Pageable pageable
    ) {

        Page<SemesterUnit> semesterUnits =
                semesterUnitRepository.findAll(
                        SemesterUnitSpecification.withFilters(
                                filter.getSearch(),
                                filter.getCourseId(),
                                filter.getCurriculumId(),
                                filter.getSemesterId()
                        ),
                        pageable
                );

        return semesterUnits.map(this::toResponse);
    }


    @Transactional
    public List<TrainerAssignmentResponse> getTrainerAssignments(Long trainerId) {

        return trainerAssignmentRepository
                .findByTrainerId(trainerId)
                .stream()
                .map(this::toTrainerAssignmentResponse)
                .toList();
    }

    @Transactional
    public List<TrainerAssignmentResponse> getMyAssignmentsV1(User user) {

        return trainerAssignmentRepository
                .findByTrainerId(user.getId())
                .stream()
                .map(this::toTrainerAssignmentResponse)
                .toList();
    }

    private TrainerAllocationResponse toResponse(SemesterUnit semesterUnit) {

        TrainerAssignment assignment =
                trainerAssignmentRepository
                        .findBySemesterUnitIdAndEffectiveToIsNull(
                                semesterUnit.getId()
                        )
                        .orElse(null);

        Semester semester = semesterUnit.getSemester();
        CourseCurriculum curriculum = semester.getCourseCurriculum();

        return TrainerAllocationResponse.builder()

                .semesterUnitId(semesterUnit.getId())

                .unitId(semesterUnit.getUnit().getId())
                .unitCode(semesterUnit.getUnit().getCode())
                .unitName(semesterUnit.getUnit().getName())

                .semesterId(semester.getId())
                .semesterName(semester.getName())

                .curriculumId(curriculum.getId())
                .curriculumName(curriculum.getName())

                .courseId(curriculum.getCourse().getId())
                .courseName(curriculum.getCourse().getName())

                .trainerAssignmentId(
                        assignment != null ? assignment.getId() : null
                )

                .trainerId(
                        assignment != null ? assignment.getTrainer().getId() : null
                )

                .trainerName(
                        assignment != null ? assignment.getTrainer().getFullName() : null
                )

                .effectiveFrom(
                        assignment != null ? assignment.getEffectiveFrom() : null
                )

                .effectiveTo(
                        assignment != null ? assignment.getEffectiveTo() : null
                )

                .assigned(assignment != null)

                .build();
    }


    private TrainerAssignmentResponse toTrainerAssignmentResponse(
            TrainerAssignment assignment
    ) {

        SemesterUnit semesterUnit = assignment.getSemesterUnit();
        Semester semester = semesterUnit.getSemester();
        CourseCurriculum curriculum = semester.getCourseCurriculum();

        return TrainerAssignmentResponse.builder()
                .id(assignment.getId())

                .trainerId(assignment.getTrainer().getId())
                .trainerName(assignment.getTrainer().getFullName())

                .semesterUnitId(semesterUnit.getId())

                .unitId(semesterUnit.getUnit().getId())
                .unitCode(semesterUnit.getUnit().getCode())
                .unitName(semesterUnit.getUnit().getName())

                .semesterId(semester.getId())
                .semesterName(semester.getName())

                .curriculumId(curriculum.getId())
                .curriculumName(curriculum.getName())

                .courseId(curriculum.getCourse().getId())
                .courseName(curriculum.getCourse().getName())

                .effectiveFrom(assignment.getEffectiveFrom())
                .effectiveTo(assignment.getEffectiveTo())

                .active(assignment.isActive())

                .assignedAt(assignment.getAssignedAt())
                .assignedById(assignment.getAssignedBy().getId())
                .assignedByName(assignment.getAssignedBy().getFullName())

                .build();
    }

    @Transactional
    public void assignTrainer(
            TrainerAssignmentRequest request,
            @AuthenticationPrincipal User user
    ) throws ChangeSetPersister.NotFoundException {



        SemesterUnit semesterUnit =
                semesterUnitRepository.findById(request.getSemesterUnitId())
                        .orElseThrow(ChangeSetPersister.NotFoundException::new);

        User trainer =
                userRepository.findById(request.getTrainerId())
                        .orElseThrow(ChangeSetPersister.NotFoundException::new);

        trainerAssignmentRepository
                .findFirstBySemesterUnitIdAndEffectiveToIsNull(
                        semesterUnit.getId()
                )
                .ifPresent(existing -> {

                    existing.endAssignment(LocalDate.now());

                    trainerAssignmentRepository.save(existing);

                });

        TrainerAssignment assignment = new TrainerAssignment();

        assignment.setSemesterUnit(semesterUnit);

        assignment.setTrainer(trainer);

        assignment.setAssignedBy(user);

        assignment.setEffectiveFrom(LocalDate.now());

        trainerAssignmentRepository.save(assignment);
    }

    @Transactional()
    public List<TrainerUnitResponse> getMyAssignmentsv3(User user) {


        return trainerAssignmentRepository
                .findByTrainerIdAndEffectiveToIsNullOrderByAssignedAtDesc(user.getId())
                .stream()
                .map(a -> {

                    SemesterUnit su = a.getSemesterUnit();

                    Semester semester = su.getSemester();

                    CourseCurriculum curriculum = semester.getCourseCurriculum();

                    return TrainerUnitResponse.builder()

                            .assignmentId(a.getId())

                            .semesterUnitId(su.getId())

                            .semesterId(semester.getId())

                            .course(curriculum.getCourse().getName())

                            .curriculum(curriculum.getName())

                            .semester(semester.getName())

                            .unitCode(su.getUnit().getCode())

                            .unitName(su.getUnit().getName())

                            .build();
                })
                .toList();
    }


    @Transactional()
    public List<TrainerAssignmentResponse> getMyAssignments(User user) {

        return trainerAssignmentRepository
                .findByTrainerIdAndEffectiveToIsNullOrderByAssignedAtDesc(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    private TrainerAssignmentResponse map(TrainerAssignment assignment) {

        SemesterUnit su = assignment.getSemesterUnit();

        Semester semester = su.getSemester();

        CourseCurriculum curriculum = semester.getCourseCurriculum();

        return TrainerAssignmentResponse.builder()

                .id(assignment.getId())

                .trainerId(assignment.getTrainer().getId())
                .trainerName(assignment.getTrainer().getFullName())

                .semesterUnitId(su.getId())

                .unitId(su.getUnit().getId())
                .unitCode(su.getUnit().getCode())
                .unitName(su.getUnit().getName())

                .semesterId(semester.getId())
                .semesterName(semester.getName())

                .curriculumId(curriculum.getId())
                .curriculumName(curriculum.getName())

                .courseId(curriculum.getCourse().getId())
                .courseName(curriculum.getCourse().getName())

                .effectiveFrom(assignment.getEffectiveFrom())
                .effectiveTo(assignment.getEffectiveTo())

                .active(assignment.isActive())

                .assignedAt(assignment.getAssignedAt())

                .assignedById(
                        assignment.getAssignedBy() != null
                                ? assignment.getAssignedBy().getId()
                                : null
                )

                .assignedByName(
                        assignment.getAssignedBy() != null
                                ? assignment.getAssignedBy().getFullName()
                                : null
                )

                .build();
    }
}
