package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.AcademicPeriod;
import com.owuor.educue.academics.enums.AcademicPeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/** Persistence and uniqueness lookups for academic periods. */
public interface AcademicPeriodRepository extends
        JpaRepository<AcademicPeriod, Long>, JpaSpecificationExecutor<AcademicPeriod> {

    Optional<AcademicPeriod> findByUuid(UUID uuid);
    Optional<AcademicPeriod> findByPeriodTypeAndYearNumberAndPeriodNumber(
            AcademicPeriodType periodType,
            Integer yearNumber,
            Integer periodNumber
    );
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndUuidNot(String code, UUID uuid);
}
