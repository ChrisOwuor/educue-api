package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseAcademicPeriodRepository extends JpaRepository<CourseAcademicPeriod, Long> {

    @EntityGraph(attributePaths = {"course", "academicPeriod"})
    Optional<CourseAcademicPeriod> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"academicPeriod", "nextPeriod"})
    List<CourseAcademicPeriod> findByCourseIdOrderByPosition(Long courseId);
}
