package com.banking.notification.subscriber;

import com.banking.notification.dto.EmailRequest;
import com.banking.notification.entity.NotificationChannel;
import com.banking.notification.router.EmailRouter;
import com.banking.notification.service.NotificationAuditService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSubscriberService {

    private final EmailRouter router;
    private final Validator validator;
    private final NotificationAuditService auditService;
    @Value("${mail.from.name}")
    private String fromName;

    @Bean
    public Consumer<EmailRequest> processEmail() {
        return request -> {
            if (request == null) {
                log.warn("Received null email message, skipping");
                return;
            }
            final Set<ConstraintViolation<EmailRequest>> violations = this.validator.validate(request);
            if (!violations.isEmpty()) {
                log.error("Invalid email message eventId: {} violations: {}",
                        request.eventId(), violations.stream().map(ConstraintViolation::getMessage).sorted().toList());
                return;
            }
            final NotificationChannel channel = NotificationChannel.EMAIL;
            if (this.auditService.isAlreadySent(request.eventId(), channel)) {
                log.info("Duplicate event ignored eventId: {}", request.eventId());
                return;
            }
            log.info("Received email eventId: {} type: {}", request.eventId(), request.type());
            final UUID auditId;
            try {
                auditId = this.auditService.start(request.eventId(), fromName, request.recipient().to(), request.type(), channel);
            } catch (final DataIntegrityViolationException e) {
                log.warn("Concurrent duplicate eventId: {}, skipping", request.eventId());
                return;
            }
            try {
                this.router.send(request, request.type());
                this.auditService.markSent(auditId);
            } catch (final RuntimeException e) {
                this.auditService.markFailed(auditId, e);
                throw e;
            }
        };
    }
}