package com.owuor.educue.roles.controller;

import com.owuor.educue.roles.dto.CreatePermissionRequest;
import com.owuor.educue.roles.dto.CreateRoleRequest;
import com.owuor.educue.roles.dto.PermissionResponse;
import com.owuor.educue.roles.dto.RoleResponse;
import com.owuor.educue.roles.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasAuthority('manage_users')")
    @GetMapping
    public List<RoleResponse> getAllRoles() {
        return roleService.getAllRoles();
    }

    @PreAuthorize("hasAuthority('manage_users')")
    @GetMapping("/{id}")
    public RoleResponse getRole(@PathVariable Long id) {
        return roleService.getRole(id);
    }

    @PreAuthorize("hasAuthority('manage_users')")
    @GetMapping("/permissions")
    public List<PermissionResponse> getPermissions() {
        return roleService.getAllPermissions();
    }

    // NEW: create a permission from the UI
    @PreAuthorize("hasAuthority('manage_users')")
    @PostMapping("/permissions")
    public PermissionResponse createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        return roleService.createPermission(request);
    }

    @PreAuthorize("hasAuthority('manage_users')")
    @PostMapping
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.createRole(request);
    }

    @PreAuthorize("hasAuthority('manage_users')")
    @PutMapping("/{id}/permissions")
    public RoleResponse updatePermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds
    ) {
        return roleService.updateRolePermissions(id, permissionIds);
    }

    @PreAuthorize("hasAuthority('manage_users')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleService.deleteRole(id);
    }
}
