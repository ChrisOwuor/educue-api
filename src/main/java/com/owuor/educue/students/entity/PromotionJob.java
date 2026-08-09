package com.owuor.educue.students.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotion_jobs")
@Getter
@Setter
public class PromotionJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    /** PENDING | PROCESSING | COMPLETED | SKIPPED | FAILED | DEAD_LETTER */
    @Column(nullable = false, length = 20)
    private String status;

    /** Set when status = SKIPPED (e.g. "Pending Core Units") */
    @Column(name = "skip_reason", length = 255)
    private String skipReason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
