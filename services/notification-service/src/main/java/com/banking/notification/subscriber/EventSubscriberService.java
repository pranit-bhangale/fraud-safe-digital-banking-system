package com.banking.notification.subscriber;

import com.banking.notification.dto.EmailRequest;
import com.banking.notification.entity.NotificationChannel;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.event.NotificationEvent;
import com.banking.notification.exception.EmailPermanentDeliveryException;
import com.banking.notification.exception.EmailValidationException;
import com.banking.notification.exception.InvalidEmailEventException;
import com.banking.notification.mapper.NotificationEventMapper;
import com.banking.notification.router.EmailRouter;
import com.banking.notification.service.NotificationAuditService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
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

    private static boolean isPermanent(final RuntimeException e) {
        return e instanceof EmailValidationException
                || e instanceof EmailPermanentDeliveryException
                || e instanceof InvalidEmailEventException;
    }

    @Bean
    public Consumer<NotificationEvent> processEmail() {
        return this::handle;
    }

    private void handle(final NotificationEvent event) {
        if (event == null) {
            log.warn("Received null notification event, skipping");
            return;
        }
        log.info("Received email notification event: {}", event.eventId());
        final List<String> violations = validate(event);
        if (!violations.isEmpty()) {
            log.error("Rejected invalid event eventId: {} violations: {}", event.eventId(), violations);
            return;
        }
        if (event.email() == null || event.email().isBlank()) {
            log.error("Rejected event without recipient email eventId: {}", event.eventId());
            return;
        }
        final NotificationType type;
        try {
            type = this.mapper.resolveType(event);
        } catch (final EmailValidationException e) {
            log.error("Rejected event eventId: {} reason: {}", event.eventId(), e.getMessage());
            return;
        }
        final Optional<UUID> started;
        try {
            started = this.auditService.startIfNotSent(event.eventId(), event.source(), event.email(), type, CHANNEL);
        } catch (final DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrent duplicate eventId: {}, skipping", event.eventId());
            return;
        }
        if (started.isEmpty()) {
            log.info("Duplicate event ignored eventId: {}", event.eventId());
            return;
        }
        final UUID auditId = started.get();
        log.info("Processing notification eventId: {} type: {} source: {}",
                event.eventId(), type, event.source());
        try {
            final EmailRequest request = this.mapper.toEmailRequest(event);
            this.router.send(request, request.type());
        } catch (final RuntimeException e) {
            recordFailure(auditId, e);
            if (isPermanent(e)) {
                log.warn("Permanent failure, not retrying eventId: {} reason: {}", event.eventId(), e.getMessage());
                return;
            }
            log.error("Transient failure eventId: {}, will retry", event.eventId(), e);
            throw e;
        }
        recordSuccess(auditId, event.eventId());
        log.info("Email notification processed successfully eventId: {}", event.eventId());
    }

    private List<String> validate(final NotificationEvent event) {
        return this.validator.validate(event)
                .stream()
                .map((ConstraintViolation<NotificationEvent> v) -> v.getPropertyPath() + " " + v.getMessage())
                .sorted()
                .toList();
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