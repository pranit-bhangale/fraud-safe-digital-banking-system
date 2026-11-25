package com.banking.fraud.dto;

import com.banking.fraud.model.FraudStatus;
import lombok.Builder;

import java.util.List;

/**
 * Composite risk evaluation result produced by fraud checks.
 */
@Builder
public record RiskResult(
        FraudStatus status,
        boolean fraud,
        int score,
        List<String> reasons,
        String action
) {
}