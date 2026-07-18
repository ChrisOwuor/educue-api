package com.owuor.educue.admissions.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Boundary for slow external admission work. It currently logs the actions;
 * this method can later invoke serverless workers without changing approval.
 */
@Slf4j
@Service
public class AdmissionPostCommitService {

    @Async("admissionTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(StudentAdmittedEvent event) {
        generateAdmissionLetter(event);
        sendWelcomeEmail(event);
    }

    void generateAdmissionLetter(StudentAdmittedEvent event) {
        log.info("Generating admission letter asynchronously: application={}, admission={}, student={}",
                event.applicationNumber(), event.admissionNumber(), event.studentName());
    }

    void sendWelcomeEmail(StudentAdmittedEvent event) {
        log.info("Sending welcome email asynchronously: email={}, student={}, course={}",
                event.email(), event.studentName(), event.courseName());
    }
}
