package com.owuor.educue.roles.service;

import com.owuor.educue.roles.dto.CreatePermissionRequest;
import com.owuor.educue.roles.dto.CreateRoleRequest;
import com.owuor.educue.roles.dto.PermissionResponse;
import com.owuor.educue.roles.dto.RoleResponse;
import com.owuor.educue.roles.entity.Permission;
import com.owuor.educue.roles.entity.Role;
import com.owuor.educue.roles.repository.PermissionRepository;
import com.owuor.educue.roles.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public RoleResponse getRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        return mapToResponse(role);
    }

    // Returns DTOs, not raw entities - never expose JPA entities directly
    // over the API (leaks internal structure, and changing the entity
    // later silently breaks the API contract with the frontend).
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(PermissionResponse::from)
                .toList();
    }

    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permission with this name already exists");
        }

        Permission permission = new Permission();
        permission.setName(request.name());
        permission.setDescription(request.description());

        return PermissionResponse.from(permissionRepository.save(permission));
    }

    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.findByName(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A role with this name already exists");
        }

        Set<Permission> permissions = request.permissionIds().stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + id)))
                .collect(Collectors.toSet());

        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissions(permissions);

        return mapToResponse(roleRepository.save(role));
    }

    public RoleResponse updateRolePermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        Set<Permission> permissions = permissionIds.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + id)))
                .collect(Collectors.toSet());

        role.setPermissions(permissions);

        return mapToResponse(roleRepository.save(role));
    }

    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        roleRepository.delete(role);
    }

    private RoleResponse mapToResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions()
                        .stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet())
        );
    }
}
