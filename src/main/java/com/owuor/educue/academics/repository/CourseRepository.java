package com.owuor.educue.academics.repository;


import com.owuor.educue.academics.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, Long> , JpaSpecificationExecutor<Course> {

    Optional<Course> findByUuid(UUID uuid);

    List<Course> findByDepartmentId(Long departmentId);

    boolean existsByCode(String code);

    /** Serializes fee-configuration changes for the same courses. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id in :ids")
    List<Course> findAllForFeeConfiguration(@Param("ids") List<Long> ids);

}
