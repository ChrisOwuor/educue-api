package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.CourseCurriculum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseCurriculumRepository
        extends JpaRepository<CourseCurriculum, Long> {

    List<CourseCurriculum> findByCourseIdOrderByCreatedAtDesc(Long courseId);


    Optional<CourseCurriculum> findByCourseIdAndDefaultForAdmissionTrue(Long id);


}
