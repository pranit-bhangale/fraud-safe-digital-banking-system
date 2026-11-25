package com.banking.transaction.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base domain exception for transaction operations.
 */
@Getter
public class TransactionException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public TransactionException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "TRANSACTION_ERROR");
    }

    public TransactionException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
