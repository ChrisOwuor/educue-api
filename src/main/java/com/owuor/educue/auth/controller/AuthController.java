package com.owuor.educue.auth.controller;

import com.owuor.educue.auth.dto.LoginRequest;
import com.owuor.educue.auth.dto.UserResponse;
import com.owuor.educue.auth.jwt.JwtAuthFilter;
import com.owuor.educue.auth.service.AuthService;
import com.owuor.educue.auth.service.PasswordResetService;
import com.owuor.educue.auth.dto.ForgotPasswordRequest;
import com.owuor.educue.auth.dto.ResetPasswordRequest;
import com.owuor.educue.users.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final String defaultAvatarUrl;
    private final PasswordResetService passwordResetService;
    private final long jwtExpirationMinutes;
    private final boolean secureCookie;


    public AuthController(AuthService authService,
                          @Value("${app.default-avatar-url:https://ui-avatars.com/api/?name=User}") String defaultAvatarUrl,
                          PasswordResetService passwordResetService,
                          @Value("${jwt.expiration-minutes:60}") long jwtExpirationMinutes,
                          @Value("${app.auth.cookie-secure:false}") boolean secureCookie) {
        this.authService = authService;
        this.defaultAvatarUrl = defaultAvatarUrl;
        this.passwordResetService = passwordResetService;
        this.jwtExpirationMinutes = jwtExpirationMinutes;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthService.LoginResult result = authService.login(request.email(), request.password(), clientIp(servletRequest));

        ResponseCookie cookie = buildAuthCookie(result.token(), jwtExpirationMinutes * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(UserResponse.from(result.user(), defaultAvatarUrl));
    }

    @GetMapping("/csrf")
    public java.util.Map<String, String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        return java.util.Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, Object>> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        var token = passwordResetService.request(request.identifier());
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("message", "If the account exists, password reset instructions have been issued.");
        token.ifPresent(value -> body.put("developmentToken", value));
        return ResponseEntity.accepted().body(body);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        // If we reach this point, JwtAuthFilter already validated the
        // cookie and loaded the user - reaching here with null shouldn't
        // happen since /me is behind authenticated() in SecurityConfig.
        return ResponseEntity.ok(UserResponse.from(user, defaultAvatarUrl));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Overwrite the cookie with maxAge=0 to clear it client-side
        ResponseCookie cookie = buildAuthCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie buildAuthCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
