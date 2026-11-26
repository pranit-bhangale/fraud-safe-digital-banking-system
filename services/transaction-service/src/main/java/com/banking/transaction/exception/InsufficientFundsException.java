package com.banking.transaction.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends TransactionException {

    public InsufficientFundsException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS");
    }
}
