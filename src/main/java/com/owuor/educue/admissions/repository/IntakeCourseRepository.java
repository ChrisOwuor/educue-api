package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.IntakeCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntakeCourseRepository extends JpaRepository<IntakeCourse, Long> {
    List<IntakeCourse> findByIntakeId(Long intakeId);

    boolean existsByIntakeIdAndCourseId(Long intakeId, Long courseId);
    Optional<IntakeCourse> findByIntakeIdAndCourseId(Long intakeId, Long courseId);
}
