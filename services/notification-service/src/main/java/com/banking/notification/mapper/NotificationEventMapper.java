package com.banking.notification.mapper;

import com.banking.notification.dto.EmailRequest;
import com.banking.notification.dto.Recipient;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.event.NotificationEvent;
import com.banking.notification.exception.EmailValidationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class NotificationEventMapper {

    private static final Spec WELCOME = new Spec
            (NotificationType.WELCOME, "Welcome to Digital Banking", "welcome", List.of("name"));
    private static final Spec OTP =
            new Spec(NotificationType.OTP, "Your one-time password", "otp", List.of("otp", "validForMinutes"));
    private static final Spec TXN_SUCCESS =
            new Spec(NotificationType.TXN_SUCCESS, "Transaction successful", "txn-success", List.of("amount", "referenceNo"));
    private static final Spec TXN_FAILED =
            new Spec(NotificationType.TXN_FAILED, "Transaction failed", "txn-failed", List.of("amount", "referenceNo"));
    private static final Spec FRAUD_ALERT =
            new Spec(NotificationType.FRAUD_ALERT, "Security alert on your account", "fraud-alert", List.of("amount", "referenceNo"));

    private static final Map<String, Spec> SPECS = buildSpecs();

    private static Map<String, Spec> buildSpecs() {
        final Map<String, Spec> specs = new HashMap<>();
        for (final NotificationType type : NotificationType.values()) {
            specs.put(type.name(), specFor(type));
        }
        return Map.copyOf(specs);
    }

    private static Spec specFor(final NotificationType type) {
        return switch (type) {
            case WELCOME -> WELCOME;
            case OTP -> OTP;
            case TXN_SUCCESS -> TXN_SUCCESS;
            case TXN_FAILED -> TXN_FAILED;
            case FRAUD_ALERT -> FRAUD_ALERT;
        };
    }

    private static Spec resolveSpec(final NotificationEvent event) {
        final String raw = event.type();
        if (raw != null) {
            Spec spec = SPECS.get(raw);
            if (spec == null) spec = SPECS.get(raw.strip().toUpperCase(Locale.ROOT));
            if (spec != null) return spec;
        }
        throw new EmailValidationException("Unsupported notification type: " + raw, event.eventId());
    }

    private static void checkRequiredData(final NotificationEvent event, final Spec spec) {
        List<String> missing = null;
        for (final String key : spec.requiredData()) {
            if (isMissing(event.data().get(key))) {
                if (missing == null) {
                    missing = new ArrayList<>(2);
                }
                missing.add(key);
            }
        }
        if (missing != null) {
            throw new EmailValidationException(
                    "Missing template data " + missing + " for " + spec.type(), event.eventId());
        }
    }

    private static boolean isMissing(final Object value) {
        return value == null || (value instanceof CharSequence text && !StringUtils.hasText(text));
    }

    public EmailRequest toEmailRequest(final NotificationEvent event) {
        final String email = event.email();
        if (email == null || email.isBlank()) {
            throw new EmailValidationException("Recipient email is required for email channel", event.eventId());
        }

        final Spec spec = resolveSpec(event);
        checkRequiredData(event, spec);

        // event.data() is already an unmodifiable copy; direct constructor skips the builder
        return new EmailRequest(
                event.eventId(),
                spec.type(),
                new Recipient(email),
                spec.subject(),
                spec.template(),
                event.data());
    }

    private record Spec(NotificationType type, String subject, String template, List<String> requiredData) {
    }
}