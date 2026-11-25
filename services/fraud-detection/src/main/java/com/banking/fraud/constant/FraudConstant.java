package com.banking.fraud.constant;

import lombok.experimental.UtilityClass;

/**
 * Centralized constants for fraud detection service.
 */
@UtilityClass
public class FraudConstant {

    // Lua scripts
    public static final String SCRIPT_VELOCITY_CHECK = "fraud_velocity_check";

    // Kafka header carried on every ack: SAFE | FRAUD | ERROR
    public static final String HEADER_FRAUD_STATUS = "fraud-status";

    // Redis key prefixes
    public static final String KEY_LOCK_USER = "fraud:lock:user:";
    public static final String KEY_LOCK_CARD = "fraud:lock:card:";
    public static final String KEY_LOCK_DEVICE = "fraud:lock:device:";
    public static final String KEY_LOCK_IP = "fraud:lock:ip:";
    public static final String KEY_WINDOW_USER = "fraud:win:user:";

    // Reason codes
    public static final String REASON_USER_VELOCITY = "USER_VELOCITY";
    public static final String REASON_CARD_VELOCITY = "CARD_VELOCITY";
    public static final String REASON_DEVICE_VELOCITY = "DEVICE_VELOCITY";
    public static final String REASON_IP_VELOCITY = "IP_VELOCITY";
    public static final String REASON_BURST = "BURST_WINDOW_LIMIT";
    public static final String REASON_HIGH_AMOUNT = "HIGH_AMOUNT";
    public static final String REASON_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String REASON_CHECK_UNAVAILABLE = "CHECK_UNAVAILABLE";

    // Metrics
    public static final String METRIC_DECISIONS = "fraud.decisions";
    public static final String METRIC_DURATION = "fraud.check.duration";
}