package com.owuor.educue.users.controller;

import com.owuor.educue.users.dto.CreateUserRequest;
import com.owuor.educue.users.dto.UserResponse;
import com.owuor.educue.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAuthority('manage_users')")
    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PreAuthorize("hasAuthority('manage_users')")
    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }


    @PreAuthorize("hasAuthority('manage_users')")
    @GetMapping("/lecturers")
    public List<UserResponse> getAllLecturers() {
        return userService.getAllLecturers();
    }


    @PreAuthorize("hasAuthority('manage_users')")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PreAuthorize("hasAuthority('manage_users')")
    @PatchMapping("/{id}/active")
    public UserResponse setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return userService.setActive(id, body.getOrDefault("active", true));
    }
}
