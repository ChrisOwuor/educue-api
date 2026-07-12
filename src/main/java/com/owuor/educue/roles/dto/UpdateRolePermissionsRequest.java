package com.owuor.educue.roles.dto;

import java.util.Set;

public record UpdateRolePermissionsRequest(
        Set<Long> permissionIds
) {}
