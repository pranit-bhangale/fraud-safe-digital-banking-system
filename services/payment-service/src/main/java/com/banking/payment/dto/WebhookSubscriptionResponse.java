package com.banking.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@Schema(description = "Registered webhook subscription details")
public record WebhookSubscriptionResponse(
        @Schema(description = "Subscription unique UUID", example = "w10e8400-e29b-41d4-a716-446655440001")
        UUID id,

        @Schema(description = "Merchant unique UUID", example = "990e8400-e29b-41d4-a716-446655440004")
        UUID merchantId,

        @Schema(description = "Target webhook URL", example = "https://merchant.example.com/webhooks/payment")
        String targetUrl,

        @Schema(description = "Subscribed event types", example = "payment.completed,payment.failed")
        String eventTypes,

        @Schema(description = "Whether the subscription is currently active", example = "true")
        boolean isActive,

        @Schema(description = "Timestamp when subscription was created")
        Instant createdAt
) {
}
