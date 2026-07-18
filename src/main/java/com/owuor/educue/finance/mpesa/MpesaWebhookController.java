package com.owuor.educue.finance.mpesa;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/mpesa/callback/{token}")
@RequiredArgsConstructor
public class MpesaWebhookController {
    private final MpesaWebhookService service;
    @Value("${app.mpesa.callback-token}") private String callbackToken;

    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> validate(@PathVariable String token) {
        requireToken(token);
        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Accepted"));
    }

    @PostMapping("/confirmation")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable String token,
                                                        @RequestBody MpesaConfirmationRequest request) {
        requireToken(token);
        service.receive(request);
        return ResponseEntity.ok(Map.of("ResultCode", 0, "ResultDesc", "Accepted"));
    }

    private void requireToken(String token) {
        if (!MessageDigestSafe.equals(callbackToken, token)) throw new IllegalArgumentException("Invalid callback token");
    }

    private static final class MessageDigestSafe {
        static boolean equals(String left, String right) {
            if (left == null || right == null) return false;
            return java.security.MessageDigest.isEqual(left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    right.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
