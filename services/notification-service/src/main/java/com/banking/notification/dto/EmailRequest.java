package com.banking.notification.dto;

import com.banking.notification.entity.NotificationType;
import com.banking.notification.event.EmailEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payload for submitting a template-based email.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmailRequest(

        @NotBlank(message = "Event ID is required")
        @JsonProperty("eventId")
        String eventId,

        @NotNull(message = "Notification type is required")
        @JsonProperty("type")
        NotificationType type,

        @NotNull(message = "Recipient information is required")
        @Valid
        @JsonProperty("recipient")
        Recipient recipient,

        @NotBlank(message = "Email subject is required")
        @JsonProperty("subject")
        String subject,

        @NotBlank(message = "Template name is required")
        @JsonProperty("templateName")
        String templateName,

        @JsonProperty("templateData")
        Map<String, Object> templateData

) implements EmailEvent, Serializable {

    public EmailRequest {
        templateData = templateData != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(templateData))
                : Map.of();
    }

    public static EmailRequest ofTemplate(
            String eventId, NotificationType type, Recipient recipient,
            String subject, String templateName, Map<String, Object> templateData
    ) {
        return EmailRequest.builder()
                .eventId(eventId)
                .type(type)
                .recipient(recipient)
                .subject(subject)
                .templateName(templateName)
                .templateData(templateData)
                .build();
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    @JsonIgnore
    public String getRecipientEmail() {
        return recipient != null ? recipient.to() : null;
    }

    @Override
    public NotificationType getType() {
        return type;
    }
}
