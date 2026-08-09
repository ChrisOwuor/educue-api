package com.owuor.educue.admissions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdmissionJobWorker {
    private final AdmissionJobService jobs;
    private final AdmissionPackService packs;

    @Scheduled(fixedDelayString = "${app.admissions.worker-delay-ms:2000}")
    public void drain() {
        for (Long id : jobs.claim()) {
            try {
                var job = jobs.get(id);
                packs.generate(job.getApplicationId(), job.getStudentId());
                jobs.complete(id);
            } catch (RuntimeException e) {
                log.warn("Admission job {} failed: {}", id, e.getMessage());
                jobs.fail(id, e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.admissions.recovery-delay-ms:60000}")
    public void recover() {
        jobs.recover();
    }
}
