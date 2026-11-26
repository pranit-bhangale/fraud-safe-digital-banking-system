package com.banking.transaction.exception;

import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends TransactionException {

    public TransactionNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND");
    }
}
