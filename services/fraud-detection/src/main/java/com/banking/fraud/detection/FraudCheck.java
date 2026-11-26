package com.banking.fraud.detection;

import com.banking.fraud.dto.RiskResult;
import com.banking.fraud.event.Event;

/**
 * Abstraction interface for fraud checks.
 */
public interface FraudCheck {

    /**
     * Evaluates a payment request and returns risk evaluation result.
     */
    RiskResult check(Event.PaymentRequest request);
}
