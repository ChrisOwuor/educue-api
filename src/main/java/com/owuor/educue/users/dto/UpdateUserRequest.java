package com.owuor.educue.users.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private Long roleId;
    private Boolean active;
}
