package com.banking.payment.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record WebhookSubscriptionResponse(
        UUID id,
        UUID merchantId,
        String targetUrl,
        String eventTypes,
        boolean isActive,
        Instant createdAt
) {
}
