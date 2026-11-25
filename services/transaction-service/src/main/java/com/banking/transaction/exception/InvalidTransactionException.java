package com.banking.transaction.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends TransactionException {

    public InvalidTransactionException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION");
    }
}
