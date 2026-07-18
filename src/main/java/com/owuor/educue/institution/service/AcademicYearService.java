package com.owuor.educue.institution.service;

import com.owuor.educue.common.exception.ApiException;
import com.owuor.educue.institution.dto.AcademicYearResponse;
import com.owuor.educue.institution.dto.CreateAcademicYearRequest;
import com.owuor.educue.institution.dto.UpdateAcademicYearRequest;
import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Coordinates validation and persistence for the academic-year lifecycle. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicYearService {

    private final AcademicYearRepository repository;

    @Transactional
    public AcademicYearResponse create(CreateAcademicYearRequest request) {
        String code = normalizeCode(request.code());
        ensureUniqueCode(code, null);
        validateDates(request.startDate(), request.endDate());

        AcademicYear year = new AcademicYear();
        year.setCode(code);
        year.setStartDate(request.startDate());
        year.setStartYear(request.startDate().getYear());
        year.setEndDate(request.endDate());
        year.setClosed(Boolean.TRUE.equals(request.closed()));
        year.setActive(request.active() == null || request.active());

        if (Boolean.TRUE.equals(request.current())) {
            ensureCanBeCurrent(year);
            clearCurrentYear(null);
            year.setCurrent(true);
        }
        return AcademicYearResponse.from(repository.save(year));
    }

    public AcademicYearResponse getByUuid(UUID uuid) {
        return AcademicYearResponse.from(findEntity(uuid));
    }

    public List<AcademicYearResponse> getAll() {
        return repository.findAllByOrderByStartDateDesc().stream()
                .map(AcademicYearResponse::from)
                .toList();
    }

    public AcademicYearResponse getCurrent() {
        return repository.findByCurrentTrue()
                .map(AcademicYearResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("No current academic year is configured"));
    }

    @Transactional
    public AcademicYearResponse update(UUID uuid, UpdateAcademicYearRequest request) {
        AcademicYear year = findEntity(uuid);
        if (year.isClosed() && changesCoreDetails(year, request)) {
            throw new ApiException(HttpStatus.CONFLICT, "A closed academic year cannot have its code or dates changed");
        }

        if (request.code() != null) {
            String code = normalizeCode(request.code());
            ensureUniqueCode(code, uuid);
            year.setCode(code);
        }
        LocalDate start = request.startDate() != null ? request.startDate() : year.getStartDate();
        LocalDate end = request.endDate() != null ? request.endDate() : year.getEndDate();
        validateDates(start, end);
        year.setStartDate(start);
        year.setStartYear(start.getYear());
        year.setEndDate(end);

        if (request.closed() != null) year.setClosed(request.closed());
        if (request.active() != null) year.setActive(request.active());
        if (request.current() != null) {
            if (request.current()) {
                ensureCanBeCurrent(year);
                clearCurrentYear(uuid);
                year.setCurrent(true);
            } else {
                year.setCurrent(false);
            }
        }
        return AcademicYearResponse.from(repository.save(year));
    }

    @Transactional
    public AcademicYearResponse makeCurrent(UUID uuid) {
        AcademicYear year = findEntity(uuid);
        ensureCanBeCurrent(year);
        clearCurrentYear(uuid);
        year.setCurrent(true);
        return AcademicYearResponse.from(repository.save(year));
    }

    @Transactional
    public void delete(UUID uuid) {
        AcademicYear year = findEntity(uuid);
        if (year.isCurrent()) {
            throw new ApiException(HttpStatus.CONFLICT, "The current academic year cannot be deleted");
        }
        repository.delete(year);
    }

    private AcademicYear findEntity(UUID uuid) {
        return repository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found"));
    }

    private void clearCurrentYear(UUID replacementUuid) {
        repository.findByCurrentTrue()
                .filter(existing -> replacementUuid == null || !existing.getUuid().equals(replacementUuid))
                .ifPresent(existing -> {
                    existing.setCurrent(false);
                    repository.saveAndFlush(existing);
                });
    }

    private void ensureUniqueCode(String code, UUID uuid) {
        boolean exists = uuid == null
                ? repository.existsByCodeIgnoreCase(code)
                : repository.existsByCodeIgnoreCaseAndUuidNot(code, uuid);
        if (exists) throw new ApiException(HttpStatus.CONFLICT, "An academic year with this code already exists");
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
        }
    }

    private void ensureCanBeCurrent(AcademicYear year) {
        if (!year.isActive() || year.isClosed()) {
            throw new ApiException(HttpStatus.CONFLICT, "Only an active, open academic year can be current");
        }
    }

    private boolean changesCoreDetails(AcademicYear year, UpdateAcademicYearRequest request) {
        return (request.code() != null && !normalizeCode(request.code()).equals(year.getCode()))
                || (request.startDate() != null && !request.startDate().equals(year.getStartDate()))
                || (request.endDate() != null && !request.endDate().equals(year.getEndDate()));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
