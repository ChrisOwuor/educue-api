package com.owuor.educue.institution.dto;

import com.owuor.educue.institution.entity.Department;

public record DepartmentResponse(
        Long id,
        String name,
        String description,
        boolean active
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.isActive()
        );
    }
}
