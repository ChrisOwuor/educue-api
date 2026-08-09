package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.FeeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface FeeItemRepository extends JpaRepository<FeeItem, Long> {

    Optional<FeeItem> findByUuid(UUID uuid);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
        SELECT f
        FROM FeeItem f
        WHERE (:active IS NULL OR f.active = :active)
          AND (
              :search = ''
              OR LOWER(f.code) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(f.category, ''))
                    LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """)
    Page<FeeItem> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );


    List<FeeItem> findAllByUuidIn(
            Collection<UUID> uuids
    );

}
