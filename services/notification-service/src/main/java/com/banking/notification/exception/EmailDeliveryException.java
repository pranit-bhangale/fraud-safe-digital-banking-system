package com.banking.notification.exception;

public class EmailDeliveryException extends EmailException {
    public EmailDeliveryException(String message, String eventId, boolean retryable, Throwable cause) {
        super(message, eventId, retryable, cause);
    }
}
