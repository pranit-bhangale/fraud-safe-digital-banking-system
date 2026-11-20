package com.banking.notification.service;

import com.banking.notification.entity.NotificationAudit;
import com.banking.notification.entity.NotificationChannel;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.repository.NotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAuditServiceImpl implements NotificationAuditService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final NotificationAuditRepository repository;

    private static String truncate(final Throwable error) {
        final String msg = error.getClass().getSimpleName() + ": " + error.getMessage();
        return msg.length() > MAX_ERROR_LENGTH ? msg.substring(0, MAX_ERROR_LENGTH) : msg;
    }

    /**
     * PII safety: a***@gmail.com. Phone numbers keep only the last 4 digits.
     */
    private static String mask(final String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        final int at = value.indexOf('@');
        if (at > 1) return value.charAt(0) + "***" + value.substring(at);
        return value.length() > 4 ? "******" + value.substring(value.length() - 4) : "****";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadySent(final String eventId, final NotificationChannel channel) {
        return this.repository.findByEventIdAndChannel(eventId, channel)
                .map(a -> a.getStatus() == NotificationStatus.SENT
                        || a.getStatus() == NotificationStatus.DELIVERED)
                .orElse(false);
    }

    // REQUIRES_NEW: the audit row must commit even if the surrounding flow fails/rolls back
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID start(final String eventId, final String source, final String destination,
                      final NotificationType type, final NotificationChannel channel) {
        final NotificationAudit audit = this.repository.findByEventIdAndChannel(eventId, channel)
                .orElseGet(() -> NotificationAudit.builder()
                        .eventId(eventId)
                        .source(source)
                        .destination(mask(destination))
                        .type(type)
                        .channel(channel)
                        .attempts(0)
                        .build());
        audit.setStatus(NotificationStatus.PROCESSING);
        audit.setAttempts(audit.getAttempts() + 1);
        audit.setErrorMessage(null);
        return this.repository.save(audit).getId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(final UUID auditId) {
        this.repository.findById(auditId).ifPresent(a -> {
            a.setStatus(NotificationStatus.SENT);
            a.setErrorMessage(null);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(final UUID auditId, final Throwable error) {
        this.repository.findById(auditId).ifPresent(a -> {
            a.setStatus(NotificationStatus.FAILED);
            a.setErrorMessage(truncate(error));
        });
    }
}