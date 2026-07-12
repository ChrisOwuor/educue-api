package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.CreateTrainerAssignmentRequest;
import com.owuor.educue.academics.dto.TrainerAssignmentFilterRequest;
import com.owuor.educue.academics.dto.TrainerAssignmentResponse;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.entity.TrainerAssignment;
import com.owuor.educue.academics.repository.SemesterUnitRepository;
import com.owuor.educue.academics.repository.TrainerAssignmentRepository;
import com.owuor.educue.academics.repository.TrainerAssignmentSpecification;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainerAssignmentService {

    private final TrainerAssignmentRepository trainerAssignmentRepository;
    private final SemesterUnitRepository semesterUnitRepository;
    private final UserRepository userRepository;

    public TrainerAssignmentResponse create(
            CreateTrainerAssignmentRequest request,
            User authentication
    ) {

        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found."));

        SemesterUnit semesterUnit = semesterUnitRepository.findById(request.getSemesterUnitId())
                .orElseThrow(() -> new IllegalArgumentException("Semester unit not found."));


        //---------------------------------------------------------
        // Prevent assigning the same trainer twice
        //---------------------------------------------------------

        if (trainerAssignmentRepository.existsByTrainerIdAndSemesterUnitIdAndEffectiveToIsNull(
                trainer.getId(),
                semesterUnit.getId()
        )) {
            throw new IllegalArgumentException(
                    "This trainer is already assigned to this semester unit."
            );
        }

        //---------------------------------------------------------
        // End previous active assignment (if any)
        //---------------------------------------------------------

        trainerAssignmentRepository
                .findBySemesterUnitIdAndEffectiveToIsNull(semesterUnit.getId())
                .ifPresent(existing -> {
                    existing.endAssignment(request.getEffectiveFrom().minusDays(1));
                    trainerAssignmentRepository.save(existing);
                });

        //---------------------------------------------------------
        // Create new assignment
        //---------------------------------------------------------

        TrainerAssignment assignment = new TrainerAssignment();

        assignment.setTrainer(trainer);
        assignment.setSemesterUnit(semesterUnit);
        assignment.setEffectiveFrom(request.getEffectiveFrom());
        assignment.setAssignedBy(authentication);

        TrainerAssignment saved =
                trainerAssignmentRepository.save(assignment);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TrainerAssignmentResponse> get(
            TrainerAssignmentFilterRequest filter,
            Pageable pageable
    ) {

        Page<TrainerAssignment> assignments =
                trainerAssignmentRepository.findAll(
                        TrainerAssignmentSpecification.withFilters(
                                filter.getSearch(),
                                filter.getTrainerId(),
                                filter.getCourseId(),
                                filter.getCurriculumId(),
                                filter.getSemesterId(),
                                filter.getActiveOnly()
                        ),
                        pageable
                );

        return assignments.map(this::toResponse);
    }

    //=========================================================
    // Mapper
    //=========================================================

    private TrainerAssignmentResponse toResponse(
            TrainerAssignment assignment
    ) {

        SemesterUnit semesterUnit = assignment.getSemesterUnit();

        var semester = semesterUnit.getSemester();

        var curriculum = semester.getCourseCurriculum();

        var course = curriculum.getCourse();

        User trainer = assignment.getTrainer();

        User assignedBy = assignment.getAssignedBy();

        return TrainerAssignmentResponse.builder()
                .id(assignment.getId())

                .trainerId(trainer.getId())
                .trainerName(trainer.getFullName())

                .semesterUnitId(semesterUnit.getId())

                .unitId(semesterUnit.getUnit().getId())
                .unitCode(semesterUnit.getUnit().getCode())
                .unitName(semesterUnit.getUnit().getName())

                .semesterId(semester.getId())
                .semesterName(semester.getName())

                .curriculumId(curriculum.getId())
                .curriculumName(curriculum.getName())

                .courseId(course.getId())
                .courseName(course.getName())

                .effectiveFrom(assignment.getEffectiveFrom())
                .effectiveTo(assignment.getEffectiveTo())

                .active(assignment.isActive())

                .assignedAt(assignment.getAssignedAt())

                .assignedById(
                        assignedBy != null ? assignedBy.getId() : null
                )
                .assignedByName(
                        assignedBy != null ? assignedBy.getFullName() : null
                )

                .build();
    }


}
