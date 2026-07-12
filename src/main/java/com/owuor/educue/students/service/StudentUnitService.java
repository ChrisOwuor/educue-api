package com.owuor.educue.students.service;

import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.repository.SemesterUnitRepository;
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

    private final SemesterUnitRepository semesterUnitRepository;

    public List<StudentUnitResponse> getCurrentSemesterUnits(Long userId) {

        return semesterUnitRepository
                .findCurrentSemesterUnitsByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private StudentUnitResponse toResponse(
            SemesterUnit item
    ) {
        return StudentUnitResponse.builder()
                .semesterUnitId(item.getId())
                .semesterId(item.getSemester().getId())
                .unitId(item.getUnit().getId())
                .unitCode(item.getUnit().getCode())
                .unitName(item.getUnit().getName())
                .creditHours(item.getUnit().getCreditHours())
                .isMandatory(item.isMandatory())
                .category(item.getCategory())
                .build();
    }
}
