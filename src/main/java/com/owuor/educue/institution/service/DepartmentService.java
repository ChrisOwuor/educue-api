package com.owuor.educue.institution.service;

import com.owuor.educue.institution.dto.CreateDepartmentRequest;
import com.owuor.educue.institution.dto.DepartmentResponse;
import com.owuor.educue.institution.dto.UpdateDepartmentRequest;
import com.owuor.educue.institution.entity.Department;
import com.owuor.educue.institution.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentResponse create(CreateDepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A department with this name already exists");
        }

        Department department = new Department();
        department.setName(request.name());
        department.setDescription(request.description());
        department.setActive(true);

        return DepartmentResponse.from(departmentRepository.save(department));
    }

    public DepartmentResponse getById(Long id) {
        return DepartmentResponse.from(findEntity(id));
    }

    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll()
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    public DepartmentResponse update(Long id, UpdateDepartmentRequest request) {
        Department department = findEntity(id);

        if (request.name() != null) {
            if (!request.name().equals(department.getName()) && departmentRepository.existsByName(request.name())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A department with this name already exists");
            }
            department.setName(request.name());
        }
        if (request.description() != null) department.setDescription(request.description());
        if (request.active() != null) department.setActive(request.active());

        return DepartmentResponse.from(departmentRepository.save(department));
    }

    public void delete(Long id) {
        Department department = findEntity(id);
        departmentRepository.delete(department);
    }

    private Department findEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));
    }
}
