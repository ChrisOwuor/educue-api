package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.CourseIntakeConfiguration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseIntakeConfigurationRepository extends JpaRepository<CourseIntakeConfiguration, Long> {
    @EntityGraph(attributePaths = {"course", "intake"})
    List<CourseIntakeConfiguration> findByIntakeId(Long intakeId);
    @EntityGraph(attributePaths = {"course", "intake"})
    Optional<CourseIntakeConfiguration> findByIntakeIdAndCourseId(Long intakeId, Long courseId);
}
