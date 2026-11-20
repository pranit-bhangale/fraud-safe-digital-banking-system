package com.banking.notification.exception;

public class EmailPermanentDeliveryException extends EmailDeliveryException {
    public EmailPermanentDeliveryException(String message, String eventId, Throwable cause) {
        super(message, eventId, false, cause);
    }
}
