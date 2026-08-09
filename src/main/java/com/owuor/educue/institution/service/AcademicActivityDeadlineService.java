package com.owuor.educue.institution.service;

import com.owuor.educue.common.exception.ApiException;
import com.owuor.educue.institution.dto.*;
import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.institution.enums.AcademicActivityType;
import com.owuor.educue.institution.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicActivityDeadlineService {
    private final AcademicActivityDeadlineRepository repository;
    private final AcademicYearRepository yearRepository;
    private final Clock clock;

    @Transactional
    public AcademicActivityDeadlineResponse save(AcademicActivityDeadlineRequest request) {
        AcademicYear year = yearRepository.findByUuid(request.academicYearUuid())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found"));
        var value = repository.findByAcademicYearIdAndActivityType(year.getId(), request.activityType())
                .orElseGet(com.owuor.educue.institution.entity.AcademicActivityDeadline::new);
        if (request.startsAt() != null && request.startsAt().isAfter(request.deadlineAt()))
            throw new ApiException(HttpStatus.CONFLICT, "Activity start cannot be after its deadline");
        value.setAcademicYear(year);
        value.setActivityType(request.activityType());
        value.setStartsAt(request.startsAt());
        value.setDeadlineAt(request.deadlineAt());
        value.setDescription(request.description() == null ? null : request.description().trim());
        value.setActive(request.active() == null || request.active());
        return AcademicActivityDeadlineResponse.from(repository.save(value));
    }

    public List<AcademicActivityDeadlineResponse> list(UUID yearUuid) {
        return repository.findByAcademicYearUuidOrderByDeadlineAt(yearUuid).stream().map(AcademicActivityDeadlineResponse::from).toList();
    }

    public List<AcademicActivityDeadlineResponse> upcoming() {
        return repository.findByActiveTrueAndDeadlineAtGreaterThanEqualOrderByDeadlineAt(LocalDateTime.now(clock)).stream()
                .map(AcademicActivityDeadlineResponse::from).toList();
    }

    @Transactional
    public void delete(UUID uuid) {
        repository.delete(repository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Activity deadline not found")));
    }

    public void requireOpen(AcademicActivityType type, AcademicYear year) {
        if (year.isClosed()) throw new ApiException(HttpStatus.CONFLICT, year.getCode() + " academic year is closed");
        repository.findByAcademicYearIdAndActivityType(year.getId(), type).filter(d -> d.isActive()).ifPresent(d -> {
            LocalDateTime now = LocalDateTime.now(clock);
            if (d.getStartsAt() != null && now.isBefore(d.getStartsAt()))
                throw new ApiException(HttpStatus.CONFLICT, type.name().replace('_', ' ') + " opens on " + d.getStartsAt());
            if (now.isAfter(d.getDeadlineAt()))
                throw new ApiException(HttpStatus.CONFLICT, type.name().replace('_', ' ') + " closed on " + d.getDeadlineAt());
        });
    }
}
