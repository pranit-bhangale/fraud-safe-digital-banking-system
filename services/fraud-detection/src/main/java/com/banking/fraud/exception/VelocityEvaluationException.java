package com.banking.fraud.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when velocity evaluation encounters unexpected results or unprocessable state.
 */
public class VelocityEvaluationException extends FraudException {

    public VelocityEvaluationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "VELOCITY_EVALUATION_ERROR");
    }

    public VelocityEvaluationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "VELOCITY_EVALUATION_ERROR");
    }
}
