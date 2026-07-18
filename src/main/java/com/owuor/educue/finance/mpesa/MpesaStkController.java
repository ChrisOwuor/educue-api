package com.owuor.educue.finance.mpesa;

import tools.jackson.databind.JsonNode;
import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class MpesaStkController {
    private final MpesaStkPushService stkService;
    private final MpesaStkCallbackService callbackService;
    @Value("${app.mpesa.stk-callback-token}") private String callbackToken;

    @PostMapping("/api/student/me/payments/stk-push")
    @PreAuthorize("hasRole('STUDENT')")
    public StkPushResponse initiate(@AuthenticationPrincipal User user,
                                    @Valid @RequestBody InitiateStkPushRequest request) {
        return stkService.initiate(user.getId(), request);
    }

    @GetMapping("/api/student/me/payments/stk-push/{requestId}")
    @PreAuthorize("hasRole('STUDENT')")
    public StkPushResponse status(@AuthenticationPrincipal User user, @PathVariable UUID requestId) {
        return stkService.status(user.getId(), requestId);
    }

    @PostMapping("/api/payments/mpesa/stk/{token}/callback")
    public ResponseEntity<Map<String, Boolean>> callback(@PathVariable String token, @RequestBody JsonNode payload) {
        if (!MessageDigest.isEqual(callbackToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            throw new IllegalArgumentException("Invalid callback token");
        callbackService.receive(payload);
        return ResponseEntity.ok(Map.of("accepted", true));
    }
}
