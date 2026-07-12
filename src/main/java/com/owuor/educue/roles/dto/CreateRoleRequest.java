package com.owuor.educue.roles.dto;

import java.util.Set;

public record CreateRoleRequest(
        String name,
        String description,
        Set<Long> permissionIds
) {}
