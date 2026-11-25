package com.banking.fraud.client;

import com.banking.fraud.exception.KafkaPublishException;
import com.banking.fraud.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Lightweight, centralized Kafka client for publishing events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaClient {

    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SOURCE_SERVICE = "X-Source-Service";
    private static final String SERVICE_NAME = "fraud-detection";

    private final StreamBridge streamBridge;

    /**
     * Publishes a message asynchronously (fire-and-forget) to the target binding.
     */
    public <T> void sendAsync(String bindingName, String partitionKey, T payload) {
        sendAsync(bindingName, partitionKey, payload, Collections.emptyMap());
    }

    /**
     * Publishes a message asynchronously (fire-and-forget) with custom headers.
     */
    public <T> void sendAsync(
            String bindingName, String partitionKey,
            T payload, Map<String, Object> customHeaders
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                Message<T> message = buildMessage(partitionKey, payload, customHeaders);
                boolean sent = this.streamBridge.send(bindingName, message);
                if (sent) {
                    log.debug("Published event: binding: {} key: {}", bindingName, MaskingUtil.maskAccountNo(partitionKey));
                } else {
                    log.error("Failed to publish event: binding: {} key: {}", bindingName, MaskingUtil.maskAccountNo(partitionKey));
                }
            } catch (Exception ex) {
                log.error("Error publishing event: binding: {} key: {}: {}", bindingName, MaskingUtil.maskAccountNo(partitionKey), ex.getMessage());
            }
        });
    }

    private <T> Message<T> buildMessage(String partitionKey, T payload, Map<String, Object> customHeaders) {
        MessageBuilder<T> builder = MessageBuilder.withPayload(payload)
                .setHeader(HEADER_TIMESTAMP, System.currentTimeMillis())
                .setHeader(HEADER_SOURCE_SERVICE, SERVICE_NAME);
        if (partitionKey != null && !partitionKey.isBlank()) {
            builder.setHeader(KafkaHeaders.KEY, partitionKey.getBytes(StandardCharsets.UTF_8));
        }
        if (customHeaders != null) customHeaders.forEach(builder::setHeader);
        return builder.build();
    }

    /**
     * Publishes a message synchronously and returns whether the send succeeded.
     */
    public <T> boolean send(String bindingName, String partitionKey, T payload) {
        return send(bindingName, partitionKey, payload, Collections.emptyMap());
    }

    /**
     * Publishes a message synchronously with custom headers and returns whether the send succeeded.
     */
    public <T> boolean send(
            String bindingName, String partitionKey,
            T payload, Map<String, Object> customHeaders
    ) {
        try {
            Message<T> message = buildMessage(partitionKey, payload, customHeaders);
            return this.streamBridge.send(bindingName, message);
        } catch (Exception ex) {
            log.error("Error publishing event: binding: {} key: {}: {}", bindingName, MaskingUtil.maskAccountNo(partitionKey), ex.getMessage());
            return false;
        }
    }

    /**
     * Publishes a message synchronously, throwing KafkaPublishException on failure.
     */
    public <T> void sendOrThrow(String bindingName, String partitionKey, T payload) {
        try {
            Message<T> message = buildMessage(partitionKey, payload, Collections.emptyMap());
            boolean sent = this.streamBridge.send(bindingName, message);
            if (!sent) {
                throw new KafkaPublishException("Failed to publish message to binding: " + bindingName, null);
            }
        } catch (Exception ex) {
            if (ex instanceof KafkaPublishException kpe) {
                throw kpe;
            }
            log.error("Error publishing event: binding: {} key: {}: {}", bindingName, MaskingUtil.maskAccountNo(partitionKey), ex.getMessage());
            throw new KafkaPublishException("Failed to publish message to binding: " + bindingName, ex);
        }
    }
}