package com.banking.fraud.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when Redis connectivity, Lua script execution, or cluster commands fail.
 */
public class RedisOperationException extends FraudException {

    public RedisOperationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE, "REDIS_OPERATION_ERROR");
    }
}
