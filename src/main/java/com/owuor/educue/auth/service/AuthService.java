package com.owuor.educue.auth.service;

import com.owuor.educue.auth.jwt.JwtService;
import com.owuor.educue.common.exception.ApiException;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottleService loginThrottleService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, LoginThrottleService loginThrottleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginThrottleService = loginThrottleService;
    }

    public record LoginResult(User user, String token) {}

    public LoginResult login(String identifier, String rawPassword, String clientIp) {
        loginThrottleService.check(identifier, clientIp);
        String normalized = identifier.trim();
        User user = userRepository.findForAuthenticationByEmail(normalized)
                .or(() -> userRepository.findForAuthenticationByStudentAdmissionNumber(normalized))
                .orElse(null);
        if (user == null) { loginThrottleService.failed(identifier, clientIp); throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/registration number or password"); }

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account has been deactivated");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginThrottleService.failed(identifier, clientIp);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/registration number or password");
        }

        loginThrottleService.succeeded(identifier, clientIp);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().getName());
        return new LoginResult(user, token);
    }



}
