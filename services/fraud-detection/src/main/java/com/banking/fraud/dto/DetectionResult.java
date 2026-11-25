package com.banking.fraud.dto;

import com.banking.fraud.event.Event;
import com.banking.fraud.model.FraudStatus;
import lombok.Builder;

@Builder
public record DetectionResult(
        FraudStatus status,
        String key,
        Event ack
) {
}