package com.banking.notification.router.impl;

import com.banking.notification.dto.EmailRequest;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.event.EmailEvent;
import com.banking.notification.exception.InvalidEmailEventException;
import com.banking.notification.handler.EmailHandler;
import com.banking.notification.router.EmailRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Routes email events to their corresponding while enforcing lifecycle auditing.
 */
@Slf4j
@Component
public class EmailRouterImpl implements EmailRouter {

    private final Map<NotificationType, EmailHandler> handlers;

    public EmailRouterImpl(final List<EmailHandler> handlerList) {
        final Map<NotificationType, EmailHandler> map = new EnumMap<>(NotificationType.class);
        for (final EmailHandler handler : handlerList) {
            final EmailHandler previous = map.put(handler.supports(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate EmailHandler for purpose %s: %s and %s".formatted(
                                handler.supports(), previous.getClass().getSimpleName(), handler.getClass().getSimpleName()));
            }
        }
        this.handlers = Collections.unmodifiableMap(map);
        log.info("Registered email handlers: {}", this.handlers.keySet());
    }

    @Override
    public void send(final EmailEvent event, final NotificationType type) {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Email event and its eventId cannot be null or blank");
        }
        if (type == null) {
            throw new InvalidEmailEventException("Email purpose is required, eventId=" + event.getEventId());
        }
        if (!(event instanceof EmailRequest request)) {
            throw new InvalidEmailEventException
                    ("Unsupported event type %s, eventId: %s".formatted(
                            event.getClass().getSimpleName(), event.getEventId()));
        }
        if (request.type() != type) {
            throw new InvalidEmailEventException
                    ("Purpose mismatch: argument: %s, request: %s, eventId: %s".formatted(
                            type, request.type(), request.eventId()));
        }
        final EmailHandler handler = this.handlers.get(type);
        if (handler == null) {
            throw new InvalidEmailEventException
                    ("Unsupported email purpose: %s, eventId=%s".formatted(type, request.eventId()));
        }
        log.info("Routing email eventId: {} purpose: {} handler: {}",
                request.eventId(), type, handler.getClass().getSimpleName());
        handler.send(request);
    }
}