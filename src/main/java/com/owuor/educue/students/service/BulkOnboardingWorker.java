package com.owuor.educue.students.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owuor.educue.students.dto.CreateStudentEnrollmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BulkOnboardingWorker {

    private final BulkOnboardingJobService jobs;
    private final StudentOnboardingService onboardingService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.students.bulk-worker-delay-ms:2000}")
    public void drain() {
        for (Long id : jobs.claim()) {
            try {
                var job = jobs.get(id);
                var request = objectMapper.readValue(job.getCsvRowJson(), CreateStudentEnrollmentRequest.class);
                onboardingService.create(request);
                jobs.complete(id);
                log.debug("Bulk onboarding job {} completed", id);
            } catch (RuntimeException e) {
                log.warn("Bulk onboarding job {} failed: {}", id, e.getMessage());
                jobs.fail(id, e);
            } catch (Exception e) {
                log.warn("Bulk onboarding job {} failed (parse error): {}", id, e.getMessage());
                jobs.fail(id, new RuntimeException("Parse error: " + e.getMessage(), e));
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.students.bulk-recovery-delay-ms:60000}")
    public void recover() {
        jobs.recover();
    }
}
