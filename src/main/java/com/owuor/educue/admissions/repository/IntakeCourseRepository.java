package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.IntakeCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntakeCourseRepository extends JpaRepository<IntakeCourse, Long> {
    List<IntakeCourse> findByIntakeId(Long intakeId);

    boolean existsByIntakeIdAndCourseId(Long intakeId, Long courseId);
}
