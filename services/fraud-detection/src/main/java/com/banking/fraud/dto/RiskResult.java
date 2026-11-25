package com.banking.fraud.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RiskResult(
        boolean fraud,
        int score,
        List<String> reasons
) {
}