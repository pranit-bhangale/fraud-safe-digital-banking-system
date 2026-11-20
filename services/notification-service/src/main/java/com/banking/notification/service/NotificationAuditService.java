package com.banking.notification.service;

import com.banking.notification.entity.NotificationChannel;
import com.banking.notification.entity.NotificationType;

import java.util.Optional;
import java.util.UUID;

public interface NotificationAuditService {

    Optional<UUID> startIfNotSent(
            String eventId, String source, String destination,
            NotificationType type, NotificationChannel channel
    );

    void markSent(UUID auditId);

    void markFailed(UUID auditId, Throwable error);
}