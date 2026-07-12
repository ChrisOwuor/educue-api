package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.CreateSemesterUnitRequest;
import com.owuor.educue.academics.dto.SemesterUnitResponse;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.entity.TrainerAssignment;
import com.owuor.educue.academics.entity.Unit;
import com.owuor.educue.academics.repository.SemesterRepository;
import com.owuor.educue.academics.repository.SemesterUnitRepository;
import com.owuor.educue.academics.repository.TrainerAssignmentRepository;
import com.owuor.educue.academics.repository.UnitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SemesterUnitService {

    private final SemesterUnitRepository semesterUnitRepository;
    private final SemesterRepository semesterRepository;
    private final UnitRepository unitRepository;
    private final TrainerAssignmentRepository trainerAssignmentRepository;

    public SemesterUnitResponse create(
            CreateSemesterUnitRequest request
    ) {

        Semester semester = semesterRepository.findById(
                request.getSemesterId()
        ).orElseThrow(() ->
                new RuntimeException("Semester not found"));

        Unit unit = unitRepository.findById(
                request.getUnitId()
        ).orElseThrow(() ->
                new RuntimeException("Unit not found"));

        boolean exists =
                semesterUnitRepository.existsBySemesterIdAndUnitId(
                        semester.getId(),
                        unit.getId()
                );

        if (exists) {
            throw new RuntimeException(
                    "Unit already exists in this semester"
            );
        }

        SemesterUnit semesterUnit = new SemesterUnit();

        semesterUnit.setSemester(semester);
        semesterUnit.setUnit(unit);
        semesterUnit.setCategory(request.getCategory());
        semesterUnit.setMandatory(
                Boolean.TRUE.equals(request.getMandatory())
        );

        return map(
                semesterUnitRepository.save(semesterUnit)
        );
    }

    @Transactional()
    public List<SemesterUnitResponse> getBySemesterV1(
            Long semesterId
    ) {

        return semesterUnitRepository
                .findBySemesterIdOrderByIdAsc(semesterId)
                .stream()
                .map(this::map)
                .toList();
    }

    private SemesterUnitResponse map(
            SemesterUnit entity
    ) {

        return SemesterUnitResponse.builder()
                .id(entity.getId())
                .semesterId(entity.getSemester().getId())
                .unitId(entity.getUnit().getId())
                .unitCode(entity.getUnit().getCode())
                .unitName(entity.getUnit().getName())
                .creditHours(entity.getUnit().getCreditHours())
                .category(entity.getCategory())
                .mandatory(entity.isMandatory())
                .build();
    }

    @Transactional()
    public List<SemesterUnitResponse> getBySemester(Long semesterId) {

        List<SemesterUnit> units =
                semesterUnitRepository.findBySemesterIdOrderByIdAsc(semesterId);

        Map<Long, TrainerAssignment> assignments =
                trainerAssignmentRepository
                        .findActiveAssignmentsBySemester(semesterId)
                        .stream()
                        .collect(Collectors.toMap(
                                ta -> ta.getSemesterUnit().getId(),
                                Function.identity()
                        ));

        return units.stream()
                .map(unit -> {

                    TrainerAssignment assignment =
                            assignments.get(unit.getId());

                    return SemesterUnitResponse.builder()

                            .id(unit.getId())
                            .semesterId(unit.getSemester().getId())

                            .unitId(unit.getUnit().getId())
                            .unitCode(unit.getUnit().getCode())
                            .unitName(unit.getUnit().getName())
                            .creditHours(unit.getUnit().getCreditHours())

                            .category(unit.getCategory())
                            .mandatory(unit.isMandatory())

                            .trainerAssignmentId(
                                    assignment != null
                                            ? assignment.getId()
                                            : null
                            )

                            .trainerId(
                                    assignment != null
                                            ? assignment.getTrainer().getId()
                                            : null
                            )

                            .trainerName(
                                    assignment != null
                                            ? assignment.getTrainer().getFullName()
                                            : null
                            )

                            .build();
                })
                .toList();
    }
}
