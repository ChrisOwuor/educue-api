package com.owuor.educue.auth.service;

import com.owuor.educue.auth.entity.PasswordResetToken;
import com.owuor.educue.auth.repository.PasswordResetTokenRepository;
import com.owuor.educue.common.exception.ApiException;
import com.owuor.educue.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final PasswordResetNotifier notifier;
    @Value("${app.auth.password-reset.expose-token:false}")
    private boolean expose;

    @Transactional
    public Optional<String> request(String identifier) {
        var user = users.findByEmail(identifier.trim().toLowerCase()).or(() -> users.findByStudentAdmissionNumber(identifier.trim()));
        if (user.isEmpty() || !user.get().isActive()) return Optional.empty();
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes());
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user.get());
        token.setTokenHash(hash(raw));
        token.setCreatedAt(LocalDateTime.now(clock));
        token.setExpiresAt(LocalDateTime.now(clock).plusMinutes(30));
        tokens.save(token);
        notifier.send(user.get().getEmail(), raw);
        return expose ? Optional.of(raw) : Optional.empty();
    }

    @Transactional
    public void reset(String raw, String password) {
        var token = tokens.findByTokenHash(hash(raw)).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired."));
        LocalDateTime now = LocalDateTime.now(clock);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired.");
        token.getUser().setPasswordHash(encoder.encode(password));
        token.getUser().setMustChangePassword(false);
        token.setUsedAt(now);
        users.save(token.getUser());
        tokens.save(token);
    }

    private byte[] randomBytes() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return b;
    }

    private String hash(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
