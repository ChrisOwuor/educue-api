package com.owuor.educue.institution.repository;

import com.owuor.educue.institution.entity.AcademicActivityDeadline;
import com.owuor.educue.institution.enums.AcademicActivityType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicActivityDeadlineRepository extends JpaRepository<AcademicActivityDeadline, Long> {
    @EntityGraph(attributePaths = "academicYear") Optional<AcademicActivityDeadline> findByUuid(UUID uuid);
    @EntityGraph(attributePaths = "academicYear") Optional<AcademicActivityDeadline> findByAcademicYearIdAndActivityType(Long yearId, AcademicActivityType type);
    @EntityGraph(attributePaths = "academicYear") List<AcademicActivityDeadline> findByAcademicYearUuidOrderByDeadlineAt(UUID yearUuid);
    @EntityGraph(attributePaths = "academicYear") List<AcademicActivityDeadline> findByActiveTrueAndDeadlineAtGreaterThanEqualOrderByDeadlineAt(LocalDateTime now);
}
