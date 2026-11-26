package com.banking.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record WebhookSubscriptionRequest(
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @NotBlank(message = "Target webhook URL is required")
        String targetUrl,

        @NotBlank(message = "Secret key for HMAC signing is required")
        String secretKey,

        String eventTypes
) {
    public WebhookSubscriptionRequest {
        if (eventTypes == null || eventTypes.isBlank()) {
            eventTypes = "payment.completed,payment.failed";
        }
    }
}
