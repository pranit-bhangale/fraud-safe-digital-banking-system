package com.banking.notification.exception;

public class EmailRenderException extends EmailException {

    public EmailRenderException(String message, String eventId, Throwable cause) {
        super(message, eventId, false, cause);
    }
}