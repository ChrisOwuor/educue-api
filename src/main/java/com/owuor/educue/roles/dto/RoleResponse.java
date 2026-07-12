package com.owuor.educue.roles.dto;

import java.util.Set;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Set<String> permissions
) {}
