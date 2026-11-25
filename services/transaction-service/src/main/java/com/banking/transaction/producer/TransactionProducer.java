package com.banking.transaction.producer;

import com.banking.transaction.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Publishes payment initiated events to Kafka topic for real-time fraud scoring.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProducer {

    public static final String BINDING_TRANSACTION_INITIATED = "transactionInitiated-out-0";

    private final StreamBridge streamBridge;

    public void publishInitiatedPayment(String senderAccountNo, Event event) {
        try {
            Message<Event> message = MessageBuilder.withPayload(event)
                    .setHeader(KafkaHeaders.KEY, senderAccountNo.getBytes(StandardCharsets.UTF_8))
                    .setHeader("X-Timestamp", System.currentTimeMillis())
                    .setHeader("X-Source-Service", "transaction-service")
                    .build();
            boolean sent = this.streamBridge.send(BINDING_TRANSACTION_INITIATED, message);
            if (sent) {
                log.info("Published payment initiated event: txnId: {} sender: {}", event.request().txnId(), senderAccountNo);
            } else {
                log.error("Failed to publish payment initiated event: txnId: {} sender: {}", event.request().txnId(), senderAccountNo);
            }
        } catch (Exception ex) {
            log.error("Error publishing payment initiated event: txnId: {}: {}",
                    event != null && event.request() != null ? event.request().txnId() : "unknown", ex.getMessage(), ex);
        }
    }
}