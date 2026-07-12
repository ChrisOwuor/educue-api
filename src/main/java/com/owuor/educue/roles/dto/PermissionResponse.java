package com.owuor.educue.roles.dto;

import com.owuor.educue.roles.entity.Permission;

public record PermissionResponse(Long id, String name, String description) {
    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getName(), permission.getDescription());
    }
}
