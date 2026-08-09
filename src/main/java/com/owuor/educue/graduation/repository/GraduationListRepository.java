package com.owuor.educue.graduation.repository;

import com.owuor.educue.graduation.entity.GraduationList;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.*;

public interface GraduationListRepository extends JpaRepository<GraduationList, Long>, JpaSpecificationExecutor<GraduationList> {
    Optional<GraduationList> findByAcademicYearIdAndDepartmentId(Long yearId, Long departmentId);
    Optional<GraduationList> findByUuid(UUID uuid);
}
