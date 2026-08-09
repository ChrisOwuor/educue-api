package com.owuor.educue.students.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_onboarding_jobs")
@Getter
@Setter
public class BulkOnboardingJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    /** Raw JSON-serialised CreateStudentEnrollmentRequest */
    @Column(name = "csv_row_json", nullable = false, columnDefinition = "TEXT")
    private String csvRowJson;

    /** PENDING | PROCESSING | COMPLETED | FAILED | DEAD_LETTER */
    @Column(nullable = false, length = 20)
    private String status;

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
