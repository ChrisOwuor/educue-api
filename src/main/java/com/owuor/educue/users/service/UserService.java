package com.owuor.educue.users.service;

import com.owuor.educue.institution.entity.Department;
import com.owuor.educue.institution.repository.DepartmentRepository;
import com.owuor.educue.roles.entity.Role;
import com.owuor.educue.roles.repository.RoleRepository;
import com.owuor.educue.users.dto.CreateUserRequest;
import com.owuor.educue.users.dto.UserResponse;
import com.owuor.educue.users.dto.UpdateUserRequest;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setDepartment(department);
        user.setActive(true);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findEntity(id));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = findEntity(id);
        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findEntity(id);
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }
        String username = request.username() == null || request.username().isBlank()
                ? null : request.username().trim();
        if (username != null && userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this username already exists");
        }
        Department department = request.departmentId() == null ? null : departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setUsername(username);
        user.setDepartment(department);
        if (request.active() != null) user.setActive(request.active());
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            user.setMustChangePassword(true);
        }
        return UserResponse.from(userRepository.save(user));
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }


    @Transactional(readOnly = true)
    public List<UserResponse> getAllLecturers() {

        return userRepository.findByRoleName("TRAINER")
                .stream()
                .map(UserResponse::from)
                .toList();
    }


}
