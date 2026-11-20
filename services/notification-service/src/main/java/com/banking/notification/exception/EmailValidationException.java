package com.banking.notification.exception;

public class EmailValidationException extends EmailException {

    public EmailValidationException(String message, String eventId) {
        this(message, eventId, null);
    }

    public EmailValidationException(String message, String eventId, Throwable cause) {
        super(message, eventId, false, cause);
    }
}
