package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.SemesterResponse;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;

    public List<SemesterResponse> getByCurriculum(Long curriculumId) {

        return semesterRepository
                .findByCourseCurriculumIdOrderByYearNumberAscSemesterNumberAsc(curriculumId)
                .stream()
                .map(this::map)
                .toList();
    }

    private SemesterResponse map(Semester semester) {

        SemesterResponse response = new SemesterResponse();

        response.setId(semester.getId());
        response.setYearNumber(semester.getYearNumber());
        response.setSemesterNumber(semester.getSemesterNumber());
        response.setName(semester.getName());

        return response;
    }
}
