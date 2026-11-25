package com.banking.fraud.detection;

import com.banking.fraud.dto.DetectionResult;
import com.banking.fraud.event.Event;

/**
 * Interface for fraud detection operations.
 */
public interface FraudDetector {

    /**
     * Evaluates a payment request for fraud risk.
     */
    DetectionResult detect(Event.PaymentRequest request);
}