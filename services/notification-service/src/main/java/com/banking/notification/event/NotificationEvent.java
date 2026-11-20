package com.banking.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationEvent(

        @NotBlank(message = "Event ID is required")
        String eventId,

        @NotBlank(message = "Notification type is required")
        String type,

        @NotBlank(message = "Source is required")
        String source,

        @NotNull(message = "Occurred timestamp is required")
        Instant occurredAt,

        @Email(message = "Recipient email must be valid")
        String email,

        Map<String, Object> data

) {

    public NotificationEvent {
        data = data != null ? Collections.unmodifiableMap(new LinkedHashMap<>(data)) : Map.of();
    }

    public static NotificationEvent of(
            final String eventId, final String type, final String source,
            final String email, final Map<String, Object> data
    ) {
        return NotificationEvent.builder()
                .eventId(eventId)
                .type(type)
                .source(source)
                .occurredAt(Instant.now())
                .email(email)
                .data(data)
                .build();
    }
}
