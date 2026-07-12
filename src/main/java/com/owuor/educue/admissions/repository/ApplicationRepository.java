package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByIntakeId(Long intakeId);
    List<Application> findByStatus(ApplicationStatus status);
}
