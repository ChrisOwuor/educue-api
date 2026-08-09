package com.owuor.educue.students.repository;

import com.owuor.educue.students.entity.PromotionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PromotionJobRepository extends JpaRepository<PromotionJob, Long> {

    /** Atomically claim up to `limit` pending/retry jobs. */
    @Query(value = """
            WITH c AS (
              SELECT id FROM promotion_jobs
              WHERE status IN ('PENDING','RETRY') AND next_attempt_at <= CURRENT_TIMESTAMP
              ORDER BY created_at, id
              FOR UPDATE SKIP LOCKED
              LIMIT :limit
            )
            UPDATE promotion_jobs j
            SET status = 'PROCESSING',
                locked_at = CURRENT_TIMESTAMP,
                attempt_count = attempt_count + 1,
                updated_at = CURRENT_TIMESTAMP
            FROM c WHERE j.id = c.id
            RETURNING j.id
            """, nativeQuery = true)
    List<Long> claimBatch(int limit);

    long countByStatus(String status);

    List<PromotionJob> findByBatchId(UUID batchId);

    long countByBatchIdAndStatus(UUID batchId, String status);

    @Modifying
    @Query(value = """
            UPDATE promotion_jobs
            SET status = 'RETRY', locked_at = NULL, next_attempt_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE status = 'PROCESSING' AND locked_at < :cutoff
            """, nativeQuery = true)
    void recoverStale(LocalDateTime cutoff);
}
