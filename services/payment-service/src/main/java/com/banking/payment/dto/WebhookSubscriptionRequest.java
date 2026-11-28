package com.banking.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(description = "Merchant webhook subscription registration payload")
public record WebhookSubscriptionRequest(
        @Schema(description = "Merchant unique UUID", example = "990e8400-e29b-41d4-a716-446655440004", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @Schema(description = "Target HTTPS endpoint URL for webhook POST requests", example = "https://merchant.example.com/webhooks/payment", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Target webhook URL is required")
        String targetUrl,

        @Schema(description = "Shared secret key used to compute HMAC-SHA256 signatures", example = "whsec_abcdefghijklmnopqrstuvwxyz123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Secret key for HMAC signing is required")
        String secretKey,

        @Schema(description = "Comma-delimited event types to listen for", example = "payment.completed,payment.failed", defaultValue = "payment.completed,payment.failed")
        String eventTypes
) {
    public WebhookSubscriptionRequest {
        if (eventTypes == null || eventTypes.isBlank()) {
            eventTypes = "payment.completed,payment.failed";
        }
    }
}
