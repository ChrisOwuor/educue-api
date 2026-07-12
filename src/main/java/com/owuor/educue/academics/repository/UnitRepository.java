package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.entity.Unit;
import com.owuor.educue.students.dto.StudentUnitResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnitRepository
        extends JpaRepository<Unit, Long>,
        JpaSpecificationExecutor<Unit> {

    Optional<Unit> findByCode(String code);

    boolean existsByCode(String code);

    Long countByDepartmentId(Long id);



}
