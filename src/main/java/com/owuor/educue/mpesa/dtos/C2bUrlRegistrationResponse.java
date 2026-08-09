package com.owuor.educue.mpesa.dtos;

import java.time.LocalDateTime;

public record C2bUrlRegistrationResponse(
        String shortCode,
        String responseType,
        String validationUrl,
        String confirmationUrl,
        String responseCode,
        String responseDescription,
        String originatorConversationId,
        LocalDateTime registeredAt
) {}
