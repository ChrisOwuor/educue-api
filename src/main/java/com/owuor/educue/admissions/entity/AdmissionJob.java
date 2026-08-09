package com.owuor.educue.admissions.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admission_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_admission_jobs_application_job_type",
                        columnNames = {
                                "application_id",
                                "job_type"
                        }
                )
        }
)
@Getter
@Setter
public class AdmissionJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "job_type", nullable = false)
    private String jobType;
    @Column(nullable = false)
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
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
