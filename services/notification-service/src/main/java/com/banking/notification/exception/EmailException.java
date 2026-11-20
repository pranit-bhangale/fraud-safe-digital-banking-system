package com.banking.notification.exception;

import com.banking.notification.shared.exception.BaseException;
import lombok.Getter;

@Getter
public abstract class EmailException extends BaseException {

    private final String eventId;
    private final boolean retryable;

    protected EmailException(String message, String eventId, boolean retryable, Throwable cause) {
        super("[eventId=%s] %s".formatted(eventId, message), cause);
        this.eventId = eventId;
        this.retryable = retryable;
    }
}