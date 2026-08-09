package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    List<Application> findByIntakeCourseIntakeId(Long intakeId);
    List<Application> findByStatus(ApplicationStatus status);
    List<Application> findByNationalIdIgnoreCaseOrderBySubmittedAtDesc(String nationalId);
    @Query("select a.status, count(a) from Application a group by a.status")
    List<Object[]> countByStatus();
}
