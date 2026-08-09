package com.owuor.educue.students.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionWorker {

    private final PromotionJobService jobs;
    private final StudentPromotionService promotionService;

    @Scheduled(fixedDelayString = "${app.students.promotion-worker-delay-ms:2000}")
    public void drain() {
        for (Long id : jobs.claim()) {
            try {
                var job = jobs.get(id);
                String skipReason = promotionService.promoteSingle(job.getEnrollmentId());
                if (skipReason != null) {
                    log.info("Promotion job {} skipped: {}", id, skipReason);
                    jobs.skip(id, skipReason);
                } else {
                    log.info("Promotion job {} completed for enrollment {}", id, job.getEnrollmentId());
                    jobs.complete(id);
                }
            } catch (RuntimeException e) {
                log.warn("Promotion job {} failed: {}", id, e.getMessage());
                jobs.fail(id, e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.students.promotion-recovery-delay-ms:60000}")
    public void recover() {
        jobs.recover();
    }
}
