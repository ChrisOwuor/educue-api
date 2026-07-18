package com.owuor.educue.auth.controller;

import com.owuor.educue.auth.dto.LoginRequest;
import com.owuor.educue.auth.dto.UserResponse;
import com.owuor.educue.auth.jwt.JwtAuthFilter;
import com.owuor.educue.auth.service.AuthService;
import com.owuor.educue.users.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;


    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());

        ResponseCookie cookie = buildAuthCookie(result.token(), 60000 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(UserResponse.from(result.user()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        // If we reach this point, JwtAuthFilter already validated the
        // cookie and loaded the user - reaching here with null shouldn't
        // happen since /me is behind authenticated() in SecurityConfig.
        return ResponseEntity.ok(UserResponse.from(user));
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
                .secure(false)        // requires HTTPS - fine for prod, see note below for local dev
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
