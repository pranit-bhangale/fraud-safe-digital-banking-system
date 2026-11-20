package com.banking.notification.event;

import com.banking.notification.entity.NotificationType;

/**
 * Common contract for all email notification events across the system.
 */
public interface EmailEvent {

    String getEventId();

    String getRecipientEmail();

    NotificationType getType();
}
