package com.banking.notification.subscriber;

import com.banking.notification.dto.EmailRequest;
import com.banking.notification.entity.NotificationChannel;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.event.NotificationEvent;
import com.banking.notification.mapper.NotificationEventMapper;
import com.banking.notification.router.EmailRouter;
import com.banking.notification.service.NotificationAuditService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSubscriberService {

    private static final NotificationChannel CHANNEL = NotificationChannel.EMAIL;

    private final EmailRouter router;
    private final Validator validator;
    private final NotificationAuditService auditService;
    private final NotificationEventMapper mapper;

    @Bean
    public Consumer<NotificationEvent> processEmail() {
        return this::handle;
    }

    private void handle(final NotificationEvent event) {
        if (event == null) {
            log.warn("Received null notification event, skipping");
            return;
        }
        if (!isValid(event)) return;
        final Optional<UUID> started;
        try {
            started = this.auditService.startIfNotSent(
                    event.eventId(), event.source(), event.email(), NotificationType.valueOf(event.type()), CHANNEL
            );
        } catch (final DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate eventId: {}, skipping", event.eventId());
            return;
        }
        if (started.isEmpty()) {
            log.info("Duplicate event ignored eventId: {}", event.eventId());
            return;
        }
        final UUID auditId = started.get();
        log.info("Processing notification eventId: {} type: {} source: {}",
                event.eventId(), event.type(), event.source());
        try {
            final EmailRequest request = this.mapper.toEmailRequest(event);
            this.router.send(request, request.type());
        } catch (final RuntimeException e) {
            recordFailure(auditId, e);
            throw e;
        }
        recordSuccess(auditId, event.eventId());
    }

    private boolean isValid(final NotificationEvent event) {
        final Set<ConstraintViolation<NotificationEvent>> violations = this.validator.validate(event);
        if (violations.isEmpty()) return true;
        log.error("Invalid notification event eventId: {} violations: {}", event.eventId(), violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList());
        return false;
    }

    private void recordSuccess(final UUID auditId, final String eventId) {
        try {
            this.auditService.markSent(auditId);
        } catch (final RuntimeException e) {
            log.error("Email sent but audit update failed eventId: {} auditId: {}", eventId, auditId, e);
        }
    }

    private void recordFailure(final UUID auditId, final RuntimeException cause) {
        try {
            this.auditService.markFailed(auditId, cause);
        } catch (final RuntimeException auditError) {
            cause.addSuppressed(auditError);
            log.error("Audit failure update failed auditId: {}", auditId, auditError);
        }
    }
}