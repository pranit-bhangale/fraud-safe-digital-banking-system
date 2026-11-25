package com.banking.fraud.constant;

import lombok.experimental.UtilityClass;

/**
 * Constants for account-to-account fraud detection service.
 */
@UtilityClass
public class FraudConstant {

    // Lua script name
    public static final String SCRIPT_VELOCITY_CHECK = "fraud_velocity_check";

    // Kafka Headers
    public static final String HEADER_FRAUD_STATUS = "fraud-status";
    public static final String HEADER_FRAUD_ACTION = "fraud-action";
    public static final String HEADER_RISK_SCORE = "fraud-risk-score";
    public static final String HEADER_TXN_ID = "txn-id";
    public static final String HEADER_SENDER_ACCOUNT = "sender-account-no";
    public static final String HEADER_RECEIVER_ACCOUNT = "receiver-account-no";

    // Kafka Topics
    public static final String TOPIC_PAYMENT_INITIATED = "banking.payment.initiated.v1";
    public static final String TOPIC_PAYMENT_FRAUD_ACK = "banking.payment.fraud-ack.v1";
    public static final String TOPIC_FRAUD_ALERTS = "banking.fraud.alerts.v1";

    // Redis key templates with Redis Cluster hash tags {senderAccountNo}
    public static final String KEY_LOCK_SENDER = "fraud:lock:{%s}:sender";
    public static final String KEY_LOCK_RECEIVER = "fraud:lock:{%s}:receiver:%s";
    public static final String KEY_WINDOW_SENDER = "fraud:win:{%s}:txns";

    // Velocity violation reasons
    public static final String REASON_SENDER_VELOCITY = "SENDER_ACCOUNT_VELOCITY_LOCK";
    public static final String REASON_RECEIVER_VELOCITY = "RECEIVER_ACCOUNT_VELOCITY_LOCK";
    public static final String REASON_BURST = "BURST_WINDOW_LIMIT_EXCEEDED";
    public static final String REASON_INVALID_REQUEST = "INVALID_PAYMENT_REQUEST";
    public static final String REASON_CHECK_UNAVAILABLE = "FRAUD_CHECK_UNAVAILABLE";

    // Standard Actions
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT_BLOCK = "REJECT_BLOCK";
}