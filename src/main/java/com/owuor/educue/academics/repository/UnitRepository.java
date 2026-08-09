package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository
        extends JpaRepository<Unit, Long>,
        JpaSpecificationExecutor<Unit> {

    Optional<Unit> findByCode(String code);

    Optional<Unit> findByUuid(UUID uuid);

    boolean existsByCode(String code);

    Long countByDepartmentId(Long id);

    List<Unit> findAllByUuidIn(
            Collection<UUID> uuids
    );




}
