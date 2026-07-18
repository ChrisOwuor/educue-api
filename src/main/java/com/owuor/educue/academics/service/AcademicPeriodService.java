package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.AcademicPeriodResponse;
import com.owuor.educue.academics.dto.CreateAcademicPeriodRequest;
import com.owuor.educue.academics.dto.UpdateAcademicPeriodRequest;
import com.owuor.educue.academics.entity.AcademicPeriod;
import com.owuor.educue.academics.enums.AcademicPeriodType;
import com.owuor.educue.academics.repository.AcademicPeriodRepository;
import com.owuor.educue.common.exception.ApiException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Implements creation, lookup, update and deletion rules for academic periods. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicPeriodService {

    private final AcademicPeriodRepository repository;

    @Transactional
    public AcademicPeriodResponse create(CreateAcademicPeriodRequest request) {
        String code = normalizeRequired(request.code(), "code").toUpperCase();
        String name = normalizeRequired(request.name(), "name");
        ensureUnique(code, request.periodType(), request.yearNumber(), request.periodNumber(), null);

        AcademicPeriod period = new AcademicPeriod();
        period.setCode(code);
        period.setName(name);
        period.setPeriodType(request.periodType());
        period.setYearNumber(request.yearNumber());
        period.setPeriodNumber(request.periodNumber());
        period.setSequenceNumber(request.sequenceNumber());
        period.setActive(request.active() == null || request.active());
        return AcademicPeriodResponse.from(repository.save(period));
    }

    public AcademicPeriodResponse getByUuid(UUID uuid) {
        return AcademicPeriodResponse.from(findEntity(uuid));
    }

    /** Returns periods in display order and optionally filters by type and active state. */
    public List<AcademicPeriodResponse> getAll(AcademicPeriodType periodType, Boolean active) {
        Specification<AcademicPeriod> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (periodType != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("periodType"), periodType));
        }
        if (active != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        Sort sort = Sort.by("sequenceNumber").ascending().and(Sort.by("code").ascending());
        return repository.findAll(specification, sort).stream()
                .map(AcademicPeriodResponse::from)
                .toList();
    }

    @Transactional
    public AcademicPeriodResponse update(UUID uuid, UpdateAcademicPeriodRequest request) {
        AcademicPeriod period = findEntity(uuid);
        String code = request.code() == null
                ? period.getCode()
                : normalizeRequired(request.code(), "code").toUpperCase();
        String name = request.name() == null
                ? period.getName()
                : normalizeRequired(request.name(), "name");
        AcademicPeriodType type = request.periodType() == null ? period.getPeriodType() : request.periodType();
        Integer year = request.yearNumber() == null ? period.getYearNumber() : request.yearNumber();
        Integer number = request.periodNumber() == null ? period.getPeriodNumber() : request.periodNumber();

        ensureUnique(code, type, year, number, uuid);
        period.setCode(code);
        period.setName(name);
        period.setPeriodType(type);
        period.setYearNumber(year);
        period.setPeriodNumber(number);
        if (request.sequenceNumber() != null) period.setSequenceNumber(request.sequenceNumber());
        if (request.active() != null) period.setActive(request.active());
        return AcademicPeriodResponse.from(repository.save(period));
    }

    @Transactional
    public void delete(UUID uuid) {
        AcademicPeriod period = findEntity(uuid);
        try {
            repository.delete(period);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Academic period cannot be deleted because it is used by a course unit placement"
            );
        }
    }

    private AcademicPeriod findEntity(UUID uuid) {
        return repository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Academic period not found"));
    }

    private void ensureUnique(
            String code,
            AcademicPeriodType type,
            Integer year,
            Integer number,
            UUID currentUuid
    ) {
        boolean duplicateCode = currentUuid == null
                ? repository.existsByCodeIgnoreCase(code)
                : repository.existsByCodeIgnoreCaseAndUuidNot(code, currentUuid);
        if (duplicateCode) {
            throw new ApiException(HttpStatus.CONFLICT, "An academic period with this code already exists");
        }

        repository.findByPeriodTypeAndYearNumberAndPeriodNumber(type, year, number)
                .filter(existing -> currentUuid == null || !existing.getUuid().equals(currentUuid))
                .ifPresent(existing -> {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "This period type, study year and period number combination already exists"
                    );
                });
    }

    private String normalizeRequired(String value, String field) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + " must not be blank");
        }
        return normalized;
    }
}
