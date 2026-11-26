package com.banking.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record WebhookPayload(
        String eventId,
        String eventType,
        Instant timestamp,
        Map<String, Object> data
) {
}
