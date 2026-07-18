package com.owuor.educue.institution.repository;

import com.owuor.educue.institution.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence operations and common lookups for academic years. */
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByUuid(UUID uuid);
    Optional<AcademicYear> findByCurrentTrue();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndUuidNot(String code, UUID uuid);
    List<AcademicYear> findAllByOrderByStartDateDesc();
}
