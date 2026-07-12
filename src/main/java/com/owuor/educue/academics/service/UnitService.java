package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.*;
import com.owuor.educue.academics.entity.Unit;
import com.owuor.educue.academics.repository.UnitRepository;
import com.owuor.educue.common.dto.ApiPageResponse;
import com.owuor.educue.institution.entity.Department;
import com.owuor.educue.institution.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final DepartmentRepository departmentRepository;

    public ApiPageResponse<UnitDto> getAll(UnitFilterRequest request) {

        Sort sort = Sort.by("name").ascending();

        if (request.getSort() != null) {

            String[] parts = request.getSort().split(",");

            if (parts.length == 2) {
                sort = parts[1].equalsIgnoreCase("desc")
                        ? Sort.by(parts[0]).descending()
                        : Sort.by(parts[0]).ascending();
            }
        }

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );

        Specification<Unit> spec = getUnitSpecification(request);

        Page<Unit> page =
                unitRepository.findAll(spec, pageable);

        return ApiPageResponse.<UnitDto>builder()
                .content(page.getContent()
                        .stream()
                        .map(this::toDto)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private static Specification<Unit> getUnitSpecification(UnitFilterRequest request) {
        Specification<Unit> spec = (root, query, cb) -> cb.conjunction();
        if (request.getDepartmentId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("department").get("id"),
                                    request.getDepartmentId()
                            )
            );
        }

        if (request.getStatus() != null) {

            boolean active =
                    request.getStatus().equalsIgnoreCase("active");

            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(root.get("active"), active)
            );
        }

        if (request.getSearch() != null &&
                !request.getSearch().isBlank()) {

            String search =
                    "%" + request.getSearch().toLowerCase() + "%";

            spec = spec.and(
                    (root, query, cb) ->
                            cb.or(
                                    cb.like(
                                            cb.lower(root.get("name")),
                                            search
                                    ),
                                    cb.like(
                                            cb.lower(root.get("code")),
                                            search
                                    )
                            )
            );
        }
        return spec;
    }

    public UnitDto getById(Long id) {

        Unit unit = unitRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Unit not found"
                        ));

        return toDto(unit);
    }

    public UnitDto create(CreateUnitRequest request) {

        Department department =
                departmentRepository.findById(
                                request.getDepartmentId()
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Department not found"
                                ));

        var code = generateUnitCode(department);

        Unit unit = new Unit();

        unit.setDepartment(department);
        unit.setCode(code);
        unit.setName(request.getName());
        unit.setCreditHours(request.getCreditHours());
        unit.setDescription(request.getDescription());

        return toDto(unitRepository.save(unit));
    }

    public UnitDto update(
            Long id,
            UpdateUnitRequest request
    ) {

        Unit unit = unitRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Unit not found"
                        ));

        if (request.getDepartmentId() != null) {

            Department department =
                    departmentRepository.findById(
                                    request.getDepartmentId()
                            )
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "Department not found"
                                    ));

            unit.setDepartment(department);
        }

        if (request.getCode() != null)
            unit.setCode(request.getCode());

        if (request.getName() != null)
            unit.setName(request.getName());

        if (request.getCreditHours() != null)
            unit.setCreditHours(request.getCreditHours());

        if (request.getDescription() != null)
            unit.setDescription(request.getDescription());

        if (request.getActive() != null)
            unit.setActive(request.getActive());

        return toDto(unitRepository.save(unit));
    }

    public void delete(Long id) {

        Unit unit = unitRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Unit not found"
                        ));

        unitRepository.delete(unit);
    }

    private UnitDto toDto(Unit unit) {

        return UnitDto.builder()
                .id(unit.getId())
                .code(unit.getCode())
                .name(unit.getName())
                .creditHours(unit.getCreditHours())
                .description(unit.getDescription())
                .active(unit.isActive())
                .departmentId(unit.getDepartment().getId())
                .departmentName(
                        unit.getDepartment().getName()
                )
                .build();
    }

    private String generateUnitCode(Department department) {

        String prefix = department.getName()
                .replaceAll("[^A-Za-z ]", "")
                .trim()
                .toUpperCase();

        // ICT Department -> ICT
        // Business Department -> BUS
        prefix = prefix.length() >= 3
                ? prefix.substring(0, 3)
                : prefix;

        Long count = unitRepository.countByDepartmentId(department.getId());

        long nextNumber = 100 + count;

        return prefix + nextNumber;    }



}
