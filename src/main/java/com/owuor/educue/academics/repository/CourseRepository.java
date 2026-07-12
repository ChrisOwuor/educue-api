package com.owuor.educue.academics.repository;


import com.owuor.educue.academics.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, Long> , JpaSpecificationExecutor<Course> {

    Optional<Course> findByUuid(UUID uuid);

    List<Course> findByDepartmentId(Long departmentId);

    boolean existsByCode(String code);

}
