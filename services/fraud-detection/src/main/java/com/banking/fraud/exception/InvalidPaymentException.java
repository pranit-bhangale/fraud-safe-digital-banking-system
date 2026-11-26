package com.banking.fraud.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an incoming payment request contains missing, invalid, or malformed data.
 */
public class InvalidPaymentException extends FraudException {

    public InvalidPaymentException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_REQUEST");
    }
}
