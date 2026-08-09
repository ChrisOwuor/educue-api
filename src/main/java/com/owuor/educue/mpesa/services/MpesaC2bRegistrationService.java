package com.owuor.educue.mpesa.services;

import com.owuor.educue.common.exception.ApiException;
import com.owuor.educue.mpesa.dtos.C2bUrlRegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MpesaC2bRegistrationService {
    private final MpesaDarajaClient darajaClient;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @Value("${app.mpesa.c2b-short-code}")
    private String shortCode;

    @Value("${app.mpesa.callback-token}")
    private String callbackToken;

    public C2bUrlRegistrationResponse register() {
        String callbackBase = normalizePublicBaseUrl(publicBaseUrl)
                + "/api/gateway/v2/transaction/" + callbackToken;
        String validationUrl = callbackBase + "/validation";
        String confirmationUrl = callbackBase + "/confirmation";

        MpesaDarajaClient.C2bRegistrationResponse response;
        try {
            response = darajaClient.registerC2bUrls(
                    shortCode,
                    "Completed",
                    confirmationUrl,
                    validationUrl
            );
        } catch (RestClientResponseException exception) {
            throw mapDarajaFailure(exception);
        }
        if (response == null) {
            throw new IllegalStateException("Daraja returned an empty URL registration response");
        }

        return new C2bUrlRegistrationResponse(
                shortCode,
                "Completed",
                validationUrl,
                confirmationUrl,
                response.responseCode(),
                response.responseDescription(),
                response.originatorConversationId(),
                LocalDateTime.now()
        );
    }

    private ApiException mapDarajaFailure(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        String errorCode = jsonStringField(responseBody, "errorCode");
        String errorMessage = jsonStringField(responseBody, "errorMessage");
        String requestId = jsonStringField(responseBody, "requestId");

        StringBuilder message = new StringBuilder("M-PESA URL registration failed");
        if (errorCode != null) message.append(" (").append(errorCode).append(")");
        if (errorMessage != null) message.append(": ").append(errorMessage);
        else message.append(": Daraja is currently unavailable or rejected the configuration");
        if (requestId != null) message.append(". Daraja request ID: ").append(requestId);

        return new ApiException(HttpStatus.BAD_GATEWAY, message.toString());
    }

    private String jsonStringField(String json, String field) {
        if (json == null || json.isBlank()) return null;
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(field)
                + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String normalizePublicBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("PUBLIC_BASE_URL must be configured before registering M-PESA URLs");
        }
        String normalized = value.trim().replaceAll("/+$", "");
        if (!normalized.startsWith("https://")) {
            throw new IllegalStateException("PUBLIC_BASE_URL must use HTTPS");
        }
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException("PUBLIC_BASE_URL must be publicly reachable by Safaricom");
        }
        return normalized;
    }
}
