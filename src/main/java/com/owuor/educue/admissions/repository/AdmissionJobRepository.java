package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.AdmissionJob;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdmissionJobRepository extends JpaRepository<AdmissionJob, Long> {


    @Modifying
    @Query(value = """
        INSERT INTO admission_jobs (
            application_id,
            student_id,
            job_type,
            status,
            attempt_count,
            max_attempts,
            next_attempt_at,
            created_at,
            updated_at
        )
        VALUES (
            :applicationId,
            :studentId,
            'GENERATE_PACK',
            'PENDING',
            0,
            5,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (application_id, job_type)
        DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("applicationId") Long applicationId,
            @Param("studentId") Long studentId
    );

    @Query(value = """
            WITH c AS (
                SELECT id
                FROM admission_jobs
                WHERE status IN ('PENDING', 'RETRY')
                  AND next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY created_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT :limit
            )
            UPDATE admission_jobs j
            SET status = 'PROCESSING',
                locked_at = CURRENT_TIMESTAMP,
                attempt_count = attempt_count + 1,
                updated_at = CURRENT_TIMESTAMP
            FROM c
            WHERE j.id = c.id
            RETURNING j.id
            """, nativeQuery = true)
    List<Long> claimBatch(
            @Param("limit") int limit
    );

    long countByStatus(String status);

    @Modifying
    @Query(value = """
            UPDATE admission_jobs
            SET status = 'RETRY',
                locked_at = NULL,
                next_attempt_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE status = 'PROCESSING'
              AND locked_at < :cutoff
            """, nativeQuery = true)
    int recoverStale(
            @Param("cutoff") LocalDateTime cutoff
    );
}
