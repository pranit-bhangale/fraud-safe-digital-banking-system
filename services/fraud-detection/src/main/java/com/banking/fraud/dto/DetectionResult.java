package com.banking.fraud.dto;

import com.banking.fraud.event.Event;
import com.banking.fraud.model.FraudStatus;
import lombok.Builder;

/**
 * Result of fraud evaluation with routing metadata keyed by txnId and userId.
 */
@Builder
public record DetectionResult(
        FraudStatus status,
        String txnId,
        String key,
        Event ack,
        String action,
        int riskScore
) {
}