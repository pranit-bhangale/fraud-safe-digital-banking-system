package com.banking.account.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AccountException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AccountException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "TRANSACTION_ERROR");
    }

    public AccountException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
