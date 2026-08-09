package com.owuor.educue.admissions.service;

import com.owuor.educue.admissions.entity.AdmissionJob;
import com.owuor.educue.admissions.repository.AdmissionJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdmissionJobService {
    private final AdmissionJobRepository repository;
    private final Clock clock;
    private final MeterRegistry metrics;

    @PostConstruct
    void metrics() {
        metrics.gauge("admission.jobs.pending", repository, r -> r.countByStatus("PENDING") + r.countByStatus("RETRY"));
        metrics.gauge("admission.jobs.dead_letter", repository, r -> r.countByStatus("DEAD_LETTER"));
    }

    @Transactional
    public void enqueue(
            Long applicationId,
            Long studentId
    ) {
        int inserted =
                repository.insertIfAbsent(
                        applicationId,
                        studentId
                );

        if (inserted == 1) {
            metrics.counter(
                    "admission.jobs.enqueued"
            ).increment();
        }
    }

    @Transactional
    public List<Long> claim() {
        return repository.claimBatch(20);
    }

    @Transactional(readOnly = true)
    public AdmissionJob get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public void complete(Long id) {
        AdmissionJob j = get(id);
        j.setStatus("COMPLETED");
        j.setCompletedAt(LocalDateTime.now(clock));
        j.setLockedAt(null);
        j.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(j);
        metrics.counter("admission.jobs.completed").increment();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long id, RuntimeException failure) {
        AdmissionJob j = get(id);
        boolean dead = j.getAttemptCount() >= j.getMaxAttempts();
        j.setStatus(dead ? "DEAD_LETTER" : "RETRY");
        j.setLastError(String.valueOf(failure.getMessage()).substring(0, Math.min(1000, String.valueOf(failure.getMessage()).length())));
        j.setLockedAt(null);
        j.setNextAttemptAt(LocalDateTime.now(clock).plusMinutes((long) Math.pow(2, Math.max(0, j.getAttemptCount() - 1))));
        j.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(j);
        metrics.counter(dead ? "admission.jobs.dead_lettered" : "admission.jobs.retried").increment();
    }

    @Transactional
    public void recover() {
        repository.recoverStale(LocalDateTime.now(clock).minusMinutes(10));
    }
}
