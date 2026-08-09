package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.*;
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

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicPeriodService {
    private final AcademicPeriodRepository repository;

    @Transactional
    public List<AcademicPeriodResponse> create(CreateAcademicPeriodRequest request) {
        int finalYearPeriods = request.periodsInFinalYear() == null ? request.periodsPerYear() : request.periodsInFinalYear();
        if (finalYearPeriods > request.periodsPerYear())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Final-year periods cannot exceed periods per year");
        List<AcademicPeriod> existing = repository.findByPeriodTypeOrderBySequenceNumber(request.periodType());
        int highestExistingPeriod = existing.stream().mapToInt(AcademicPeriod::getPeriodNumber).max().orElse(0);
        if (request.periodsPerYear() < highestExistingPeriod) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This structure already contains period " + highestExistingPeriod
                                                           + "; periods per year cannot be reduced below it");
        }
        int highestExistingYear = existing.stream().mapToInt(AcademicPeriod::getYearNumber).max().orElse(0);
        if (request.totalYears() < highestExistingYear) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This structure already reaches year " + highestExistingYear
                                                           + "; total years cannot be reduced while those periods exist");
        }

        existing.forEach(period -> {
            period.setPeriodsPerYear(request.periodsPerYear());
            period.setSequenceNumber(sequence(period.getYearNumber(), period.getPeriodNumber(), request.periodsPerYear()));
        });
        repository.saveAll(existing);

        Set<String> present = new HashSet<>();
        existing.forEach(period -> present.add(period.getYearNumber() + ":" + period.getPeriodNumber()));
        List<AcademicPeriod> created = new ArrayList<>();
        for (int year = 1; year <= request.totalYears(); year++) {
            int yearLimit = year == request.totalYears() ? finalYearPeriods : request.periodsPerYear();
            for (int number = 1; number <= yearLimit; number++) {
                if (present.contains(year + ":" + number)) continue;
                AcademicPeriod period = new AcademicPeriod();
                period.setPeriodType(request.periodType());
                period.setYearNumber(year);
                period.setPeriodNumber(number);
                period.setPeriodsPerYear(request.periodsPerYear());
                period.setCode("Y" + year + request.periodType().codeToken() + number);
                period.setName("Year " + year + " " + request.periodType().displayName() + " " + number);
                period.setSequenceNumber(sequence(year, number, request.periodsPerYear()));
                period.setActive(request.active() == null || request.active());
                created.add(period);
            }
        }
        repository.saveAll(created);
        return repository.findByPeriodTypeOrderBySequenceNumber(request.periodType()).stream().map(AcademicPeriodResponse::from).toList();
    }

    public AcademicPeriodResponse getByUuid(UUID uuid) {
        return AcademicPeriodResponse.from(findEntity(uuid));
    }

    public List<AcademicPeriodResponse> getAll(AcademicPeriodType type, Boolean active) {
        Specification<AcademicPeriod> spec = (root, query, cb) -> cb.conjunction();
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("periodType"), type));
        if (active != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        return repository.findAll(spec, Sort.by("periodType").and(Sort.by("sequenceNumber"))).stream().map(AcademicPeriodResponse::from).toList();
    }

    @Transactional
    public AcademicPeriodResponse update(UUID uuid, UpdateAcademicPeriodRequest request) {
        AcademicPeriod period = findEntity(uuid);
        if (request.periodType() != null && request.periodType() != period.getPeriodType()
            || request.yearNumber() != null && !request.yearNumber().equals(period.getYearNumber())
            || request.periodNumber() != null && !request.periodNumber().equals(period.getPeriodNumber()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use the period generator to change the academic structure");
        if (request.code() != null) period.setCode(request.code());
        if (request.name() != null) period.setName(request.name());
        if (request.active() != null) period.setActive(request.active());
        return AcademicPeriodResponse.from(repository.save(period));
    }

    @Transactional
    public void delete(UUID uuid) {
        try {
            repository.delete(findEntity(uuid));
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Academic period is already in use and cannot be deleted");
        }
    }

    private AcademicPeriod findEntity(UUID uuid) {
        return repository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException("Academic period not found"));
    }

    private int sequence(int year, int number, int frequency) {
        return Math.addExact(Math.multiplyExact(year - 1, frequency), number);
    }
}
