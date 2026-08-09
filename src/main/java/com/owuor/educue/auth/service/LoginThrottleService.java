package com.owuor.educue.auth.service;

import com.owuor.educue.auth.entity.LoginAttempt;
import com.owuor.educue.auth.repository.LoginAttemptRepository;
import com.owuor.educue.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class LoginThrottleService {
    private final LoginAttemptRepository repository;
    private final Clock clock;

    private String key(String identifier, String ip) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((identifier.trim().toLowerCase() + "|" + ip).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional(readOnly = true)
    public void check(String identifier, String ip) {
        repository.findById(key(identifier, ip)).filter(a -> a.getLockedUntil() != null && a.getLockedUntil().isAfter(LocalDateTime.now(clock))).ifPresent(a -> {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later.");
        });
    }

    @Transactional
    public void failed(String identifier, String ip) {
        String key = key(identifier, ip);
        LocalDateTime now = LocalDateTime.now(clock);
        LoginAttempt a = repository.findById(key).orElseGet(() -> {
            LoginAttempt n = new LoginAttempt();
            n.setKey(key);
            n.setFirstFailedAt(now);
            return n;
        });
        if (a.getFirstFailedAt() == null || a.getFirstFailedAt().isBefore(now.minusMinutes(15))) {
            a.setFailureCount(0);
            a.setFirstFailedAt(now);
        }
        a.setFailureCount(a.getFailureCount() + 1);
        if (a.getFailureCount() >= 5) a.setLockedUntil(now.plusMinutes(15));
        a.setUpdatedAt(now);
        repository.save(a);
    }

    @Transactional
    public void succeeded(String identifier, String ip) {
        repository.deleteById(key(identifier, ip));
    }
}
