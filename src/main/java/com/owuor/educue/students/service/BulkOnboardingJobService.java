package com.owuor.educue.students.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owuor.educue.students.dto.*;
import com.owuor.educue.students.entity.BulkOnboardingJob;
import com.owuor.educue.students.repository.BulkOnboardingJobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkOnboardingJobService {

    private static final String[] REQUIRED_HEADERS = {
            "admissionNumber", "fullName", "email", "phone",
            "courseId", "intakeId", "enrolledAcademicYearId",
            "currentAcademicYearId", "currentCourseAcademicPeriodId", "admissionDate"
    };

    private final BulkOnboardingJobRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final MeterRegistry metrics;

    @PostConstruct
    void registerMetrics() {
        metrics.gauge("onboarding.jobs.pending",    repository, r -> r.countByStatus("PENDING") + r.countByStatus("RETRY"));
        metrics.gauge("onboarding.jobs.dead_letter", repository, r -> r.countByStatus("DEAD_LETTER"));
    }

    /** Parse CSV, validate, create one BulkOnboardingJob per row, return batchId. */
    @Transactional
    public BulkOnboardingBatchResponse enqueueCsv(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Upload file must not be empty");

        UUID batchId = UUID.randomUUID();
        List<BulkOnboardingJob> jobs = new ArrayList<>();

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("CSV file has no header row");

            String[] headers = headerLine.trim().split(",", -1);
            Map<String, Integer> colIndex = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) colIndex.put(headers[i].trim(), i);

            for (String h : REQUIRED_HEADERS) {
                if (!colIndex.containsKey(h))
                    throw new IllegalArgumentException("CSV is missing required column: " + h);
            }

            String line;
            int rowIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                Map<String, Object> rowMap = new LinkedHashMap<>();
                colIndex.forEach((header, idx) -> rowMap.put(header, idx < cols.length ? cols[idx].trim() : ""));

                // Build a CreateStudentEnrollmentRequest-compatible JSON
                CreateStudentEnrollmentRequest req = parseRow(rowMap);
                String json = objectMapper.writeValueAsString(req);

                BulkOnboardingJob job = new BulkOnboardingJob();
                job.setBatchId(batchId);
                job.setRowIndex(rowIndex++);
                job.setCsvRowJson(json);
                job.setStatus("PENDING");
                job.setAttemptCount(0);
                job.setNextAttemptAt(LocalDateTime.now(clock));
                job.setCreatedAt(LocalDateTime.now(clock));
                job.setUpdatedAt(LocalDateTime.now(clock));
                jobs.add(job);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse CSV: " + e.getMessage(), e);
        }

        if (jobs.isEmpty()) throw new IllegalArgumentException("CSV has no data rows");
        if (jobs.size() > 1000) throw new IllegalArgumentException("CSV cannot exceed 1000 rows per upload");

        repository.saveAll(jobs);
        log.info("Enqueued {} onboarding jobs for batch {}", jobs.size(), batchId);
        return new BulkOnboardingBatchResponse(batchId, jobs.size(), "QUEUED");
    }

    @Transactional(readOnly = true)
    public BulkOnboardingBatchStatus getBatchStatus(UUID batchId) {
        var jobs = repository.findByBatchIdOrderByRowIndex(batchId);
        if (jobs.isEmpty()) throw new jakarta.persistence.EntityNotFoundException("Batch not found: " + batchId);
        long pending    = jobs.stream().filter(j -> "PENDING".equals(j.getStatus()) || "RETRY".equals(j.getStatus())).count();
        long processing = jobs.stream().filter(j -> "PROCESSING".equals(j.getStatus())).count();
        long completed  = jobs.stream().filter(j -> "COMPLETED".equals(j.getStatus())).count();
        long failed     = jobs.stream().filter(j -> "FAILED".equals(j.getStatus())).count();
        long deadLetter = jobs.stream().filter(j -> "DEAD_LETTER".equals(j.getStatus())).count();
        return new BulkOnboardingBatchStatus(batchId, jobs.size(), pending, processing, completed, failed, deadLetter);
    }

    @Transactional
    public List<Long> claim() {
        return repository.claimBatch(20);
    }

    @Transactional(readOnly = true)
    public BulkOnboardingJob get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public void complete(Long id) {
        BulkOnboardingJob j = get(id);
        j.setStatus("COMPLETED");
        j.setCompletedAt(LocalDateTime.now(clock));
        j.setLockedAt(null);
        j.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(j);
        metrics.counter("onboarding.jobs.completed").increment();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long id, RuntimeException failure) {
        BulkOnboardingJob j = get(id);
        boolean dead = j.getAttemptCount() >= j.getMaxAttempts();
        j.setStatus(dead ? "DEAD_LETTER" : "RETRY");
        String msg = failure.getMessage();
        j.setLastError(msg == null ? "unknown error" : msg.substring(0, Math.min(1000, msg.length())));
        j.setLockedAt(null);
        j.setNextAttemptAt(LocalDateTime.now(clock).plusMinutes((long) Math.pow(2, Math.max(0, j.getAttemptCount() - 1))));
        j.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(j);
        metrics.counter(dead ? "onboarding.jobs.dead_lettered" : "onboarding.jobs.retried").increment();
    }

    @Transactional
    public void recover() {
        repository.recoverStale(LocalDateTime.now(clock).minusMinutes(10));
    }

    // -----------------------------------------------------------------------
    // CSV row → DTO
    // -----------------------------------------------------------------------

    private CreateStudentEnrollmentRequest parseRow(Map<String, Object> row) {
        return new CreateStudentEnrollmentRequest(
                str(row, "admissionNumber"),
                str(row, "fullName"),
                str(row, "email"),
                strOpt(row, "phone"),
                strOpt(row, "nationalId"),
                dateOpt(row, "dateOfBirth"),
                strOpt(row, "guardianName"),
                strOpt(row, "guardianPhone"),

                longVal(row, "courseId"),
                longVal(row, "intakeId"),

                uuidVal(row, "enrolledAcademicYearUuid"),
                uuidVal(row, "currentAcademicYearUuid"),
                uuidVal(row, "currentCourseAcademicPeriodUuid"),

                date(row, "admissionDate"),
                boolOpt(row, "migrated"),
                decimalOpt(row, "openingDebit"),
                decimalOpt(row, "openingCredit"),
                dateOpt(row, "openingBalanceDate"),
                strOpt(row, "legacyReference")
        );
    }

    private UUID uuidVal(Map<String, Object> row, String key) {
        Object raw = row.get(key);

        if (raw == null) {
            throw new IllegalArgumentException(
                    "Missing required UUID field: " + key
            );
        }

        String value = raw.toString().trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                    "UUID field cannot be blank: " + key
            );
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid UUID for field '" + key + "': " + value,
                    exception
            );
        }
    }

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null || v.toString().isBlank()) throw new IllegalArgumentException("Missing required CSV column: " + key);
        return v.toString().trim();
    }

    private String strOpt(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return (v == null || v.toString().isBlank()) ? null : v.toString().trim();
    }

    private Long longVal(Map<String, Object> row, String key) {
        String s = str(row, key);
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Column " + key + " must be a numeric ID, got: " + s); }
    }

    private LocalDate date(Map<String, Object> row, String key) {
        String s = str(row, key);
        try { return LocalDate.parse(s); }
        catch (Exception e) { throw new IllegalArgumentException("Column " + key + " must be a date (yyyy-MM-dd), got: " + s); }
    }

    private LocalDate dateOpt(Map<String, Object> row, String key) {
        String s = strOpt(row, key);
        if (s == null) return null;
        try { return LocalDate.parse(s); }
        catch (Exception e) { throw new IllegalArgumentException("Column " + key + " must be a date (yyyy-MM-dd), got: " + s); }
    }

    private Boolean boolOpt(Map<String, Object> row, String key) {
        String s = strOpt(row, key);
        return s == null ? null : Boolean.parseBoolean(s);
    }

    private BigDecimal decimalOpt(Map<String, Object> row, String key) {
        String s = strOpt(row, key);
        if (s == null) return null;
        try { return new BigDecimal(s); }
        catch (Exception e) { return null; }
    }
}
