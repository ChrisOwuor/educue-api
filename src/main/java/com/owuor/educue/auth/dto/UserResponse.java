package com.owuor.educue.auth.dto;

import com.owuor.educue.users.entity.User;

// Mirrors the frontend's `User` type in src/types/user.ts exactly:
// { id, fullName, email, role }
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        String username,
        String avatarUrl,
        String departmentName
) {
    public static UserResponse from(User user, String defaultAvatarUrl) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName(),
                user.getUsername(),
                user.getAvatarUrl() == null ? defaultAvatarUrl : user.getAvatarUrl(),
                user.getDepartment() == null ? null : user.getDepartment().getName()
        );
    }
}
