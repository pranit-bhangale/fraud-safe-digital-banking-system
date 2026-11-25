package com.banking.fraud.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base domain exception for all fraud detection operations.
 */
@Getter
public class FraudException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public FraudException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "FRAUD_ERROR");
    }

    public FraudException(String message, HttpStatus status) {
        this(message, status, "FRAUD_ERROR");
    }

    public FraudException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public FraudException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
