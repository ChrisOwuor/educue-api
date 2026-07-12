package com.owuor.educue.users.dto;

import com.owuor.educue.users.entity.User;

// Mirrors the frontend's `User` type in src/types/user.ts exactly:
// { id, fullName, email, role }
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName()
        );
    }
}
