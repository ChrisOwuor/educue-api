package com.owuor.educue.users.dto;

import com.owuor.educue.users.entity.User;

public record MyProfileResponse(Long id, String fullName, String email, String phone,
                                String username, String avatarUrl, String role, String departmentName) {
    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getUsername(), user.getAvatarUrl(), user.getRole().getName(),
                user.getDepartment() == null ? null : user.getDepartment().getName());
    }
}
