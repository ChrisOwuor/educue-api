package com.owuor.educue.users.dto;

import com.owuor.educue.users.entity.User;

// Mirrors the frontend's `User` type in src/types/user.ts exactly:
// { id, fullName, email, role }
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String username,
        String role,
        Long departmentId,
        String departmentName,
        boolean active,
        boolean mustChangePassword
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getUsername(),
                user.getRole().getName(),
                user.getDepartment() == null ? null : user.getDepartment().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.isActive(),
                user.isMustChangePassword()
        );
    }
}
