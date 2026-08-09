package com.owuor.educue.institution.repository;


import com.owuor.educue.institution.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);
    Optional<Department> findByNameIgnoreCase(String name);

    boolean existsByName(String name);
}
