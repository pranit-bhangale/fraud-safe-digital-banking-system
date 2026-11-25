package com.banking.fraud.consumer;

import com.banking.fraud.detection.FraudDetector;
import com.banking.fraud.dto.DetectionResult;
import com.banking.fraud.event.Event;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

@Configuration
public class FraudDetectionConsumer {

    @Bean
    public Function<Event, Message<Event>> fraudDetectionFunction(FraudDetector fraudDetector) {
        return event -> {
            Event.PaymentRequest request = event == null ? null : event.request();
            if (request == null || request.txnId() == null || request.txnId().isBlank()) {
                return buildErrorAck(null, "MISSING_TXN_ID", "Dropped: event without payment request or txnId");
            }
            try {
                DetectionResult result = fraudDetector.detect(request);
                return buildAckMessage(result);
            } catch (Exception e) {
                return buildErrorAck(request.txnId(), "PROCESSING_ERROR", e.getMessage());
            }
        };
    }

    private Message<Event> buildAckMessage(DetectionResult result) {
        MessageBuilder<Event> builder = MessageBuilder.withPayload(result.ack());
        if (result.key() != null) {
            builder.setHeader(KafkaHeaders.KEY, result.key().getBytes(StandardCharsets.UTF_8));
        }
        builder.setHeader("fraud-status", result.status().name());
        return builder.build();
    }

    private Message<Event> buildErrorAck(String txnId, String eventIdSuffix, String reason) {
        String eventId = (txnId != null ? txnId : "unknown") + ":" + eventIdSuffix.toLowerCase();
        Event ack = Event.builder()
                .event(Event.FraudEvent.builder()
                        .eventId(eventId)
                        .txnId(txnId)
                        .userId("unknown")
                        .amount(java.math.BigDecimal.ZERO)
                        .currency("UNKNOWN")
                        .riskScore(0)
                        .reasons(List.of(reason))
                        .detectedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return MessageBuilder.withPayload(ack)
                .setHeader(KafkaHeaders.KEY, (txnId != null ? txnId : "unknown").getBytes(StandardCharsets.UTF_8))
                .setHeader("fraud-status", "ERROR")
                .build();
    }
}