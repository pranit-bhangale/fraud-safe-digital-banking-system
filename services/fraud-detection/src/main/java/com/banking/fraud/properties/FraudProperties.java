package com.banking.fraud.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "fraud")
public record FraudProperties(
        @DefaultValue("600000") long windowMs,
        @DefaultValue("5") int windowLimit,
        @DefaultValue("60") int lockTtlSeconds,
        @DefaultValue("100000") BigDecimal highAmount,
        @DefaultValue("60") int threshold,
        @DefaultValue Weights weights) {

    public FraudProperties {
        if (windowMs <= 0 || windowLimit <= 0 || lockTtlSeconds <= 0 || threshold <= 0) {
            throw new IllegalArgumentException(
                    "fraud.window-ms, window-limit, lock-ttl-seconds and threshold must be > 0");
        }
        if (highAmount == null || highAmount.signum() <= 0) {
            throw new IllegalArgumentException("fraud.high-amount must be > 0");
        }
    }

    /**
     * Score added per triggered rule. A payment is FRAUD when the total >= threshold.
     */
    public record Weights(
            @DefaultValue("40") int userVelocity,
            @DefaultValue("40") int cardVelocity,
            @DefaultValue("20") int deviceVelocity,
            @DefaultValue("10") int ipVelocity,
            @DefaultValue("30") int burst,
            @DefaultValue("30") int highAmount) {
    }
}