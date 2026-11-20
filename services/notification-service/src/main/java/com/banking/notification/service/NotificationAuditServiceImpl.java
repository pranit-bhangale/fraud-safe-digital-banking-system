package com.banking.notification.service;

import com.banking.notification.entity.NotificationAudit;
import com.banking.notification.entity.NotificationChannel;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.repository.NotificationAuditRepository;
import com.banking.notification.repository.NotificationAuditRepository.AuditState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
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

    private static boolean isDone(final NotificationStatus status) {
        return status == NotificationStatus.SENT || status == NotificationStatus.DELIVERED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> startIfNotSent(
            final String eventId, final String source, final String destination,
            final NotificationType type, final NotificationChannel channel
    ) {
        final Optional<AuditState> existing = this.repository.findStateByEventIdAndChannel(eventId, channel);
        if (existing.isPresent()) {
            final AuditState state = existing.get();
            if (isDone(state.getStatus())) {
                return Optional.empty();
            }
            // Redelivery of PROCESSING/FAILED: single UPDATE, no entity loaded
            this.repository.markProcessing(state.getId(), NotificationStatus.PROCESSING, Instant.now());
            return Optional.of(state.getId());
        }

        // First time: INSERT. saveAndFlush surfaces a unique-constraint race right here.
        final NotificationAudit audit = NotificationAudit.builder()
                .eventId(eventId)
                .source(source)
                .destination(mask(destination))
                .type(type)
                .channel(channel)
                .status(NotificationStatus.PROCESSING)
                .attempts(1)
                .build();
        return Optional.of(this.repository.saveAndFlush(audit).getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(final UUID auditId) {
        final int updated = this.repository.markSent(auditId, NotificationStatus.SENT, Instant.now());
        warnIfMissing(updated, auditId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(final UUID auditId, final Throwable error) {
        final int updated = this.repository.markFailed(
                auditId, NotificationStatus.FAILED, truncate(error), Instant.now());
        warnIfMissing(updated, auditId);
    }

    private void warnIfMissing(final int updated, final UUID auditId) {
        if (updated == 0) {
            log.warn("Audit row not found auditId: {}", auditId);
        }
    }
}