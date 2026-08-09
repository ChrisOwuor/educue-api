package com.owuor.educue.graduation.repository;
import com.owuor.educue.graduation.entity.GraduationBatch; import org.springframework.data.jpa.repository.*; import org.springframework.data.domain.*; import java.util.*;
public interface GraduationBatchRepository extends JpaRepository<GraduationBatch,Long>{
 @EntityGraph(attributePaths={"academicYear","createdBy","conferredBy"}) Page<GraduationBatch> findAllByOrderByGraduationDateDesc(Pageable pageable);
 @EntityGraph(attributePaths={"academicYear","createdBy","conferredBy"}) Optional<GraduationBatch> findByUuid(UUID uuid);
}
