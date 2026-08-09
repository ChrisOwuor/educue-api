package com.owuor.educue.students.service;

import com.owuor.educue.students.dto.PromotionBatchResponse;
import com.owuor.educue.students.dto.PromotionBatchStatus;
import com.owuor.educue.students.entity.PromotionJob;
import com.owuor.educue.students.repository.PromotionJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionJobService {

    private final PromotionJobRepository repository;
    private final Clock clock;
    private final MeterRegistry metrics;

    @PostConstruct
    void registerMetrics() {
        metrics.gauge("promotion.jobs.pending",    repository, r -> r.countByStatus("PENDING") + r.countByStatus("RETRY"));
        metrics.gauge("promotion.jobs.dead_letter", repository, r -> r.countByStatus("DEAD_LETTER"));
    }

    /** Enqueue one PromotionJob per enrollment ID, return batchId. */
    @Transactional
    public PromotionBatchResponse enqueueBatch(List<Long> enrollmentIds) {
        if (enrollmentIds == null || enrollmentIds.isEmpty())
            throw new IllegalArgumentException("Promotion batch must contain at least one enrollment");

        UUID batchId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(clock);

        List<PromotionJob> jobs = enrollmentIds.stream().map(enrollmentId -> {
            var job = new PromotionJob();
            job.setBatchId(batchId);
            job.setEnrollmentId(enrollmentId);
            job.setStatus("PENDING");
            job.setAttemptCount(0);
            job.setNextAttemptAt(now);
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            return job;
        }).toList();

        repository.saveAll(jobs);
        log.info("Enqueued {} promotion jobs for batch {}", jobs.size(), batchId);
        return new PromotionBatchResponse(batchId, jobs.size(), "QUEUED");
    }

    @Transactional(readOnly = true)
    public PromotionBatchStatus getBatchStatus(UUID batchId) {
        var jobs = repository.findByBatchId(batchId);
        if (jobs.isEmpty()) throw new jakarta.persistence.EntityNotFoundException("Batch not found: " + batchId);
        long pending    = jobs.stream().filter(j -> "PENDING".equals(j.getStatus()) || "RETRY".equals(j.getStatus())).count();
        long processing = jobs.stream().filter(j -> "PROCESSING".equals(j.getStatus())).count();
        long completed  = jobs.stream().filter(j -> "COMPLETED".equals(j.getStatus())).count();
        long skipped    = jobs.stream().filter(j -> "SKIPPED".equals(j.getStatus())).count();
        long failed     = jobs.stream().filter(j -> "FAILED".equals(j.getStatus())).count();
        long deadLetter = jobs.stream().filter(j -> "DEAD_LETTER".equals(j.getStatus())).count();
        return new PromotionBatchStatus(batchId, jobs.size(), pending, processing, completed, skipped, failed, deadLetter);
    }

    @Transactional
    public List<Long> claim() {
        return repository.claimBatch(20);
    }

    @Transactional(readOnly = true)
    public PromotionJob get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public void complete(Long id) {
        PromotionJob j = get(id);
        j.setStatus("COMPLETED");
        j.setCompletedAt(LocalDateTime.now(clock));
        j.setLockedAt(null);
        j.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(j);
        metrics.counter("promotion.jobs.completed").increment();
    }

    @Transactional
    public void skip(Long id, String reason) {
        PromotionJob j = get(id);
        j.setStatus("SKIPPED");
        j.setSkipReason(reason);
        j.setCompletedAt(LocalDateTime.now(clock));
        j.setLockedAt(null);
        j.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(j);
        metrics.counter("promotion.jobs.skipped").increment();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long id, RuntimeException failure) {
        PromotionJob j = get(id);
        boolean dead = j.getAttemptCount() >= j.getMaxAttempts();
        j.setStatus(dead ? "DEAD_LETTER" : "RETRY");
        String msg = failure.getMessage();
        j.setLastError(msg == null ? "unknown error" : msg.substring(0, Math.min(1000, msg.length())));
        j.setLockedAt(null);
        j.setNextAttemptAt(LocalDateTime.now(clock).plusMinutes((long) Math.pow(2, Math.max(0, j.getAttemptCount() - 1))));
        j.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(j);
        metrics.counter(dead ? "promotion.jobs.dead_lettered" : "promotion.jobs.retried").increment();
    }

    @Transactional
    public void recover() {
        repository.recoverStale(LocalDateTime.now(clock).minusMinutes(10));
    }
}
