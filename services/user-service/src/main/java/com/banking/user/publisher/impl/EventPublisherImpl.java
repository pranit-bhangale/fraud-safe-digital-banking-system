package com.banking.user.publisher.impl;

import com.banking.user.event.NotificationEvent;
import com.banking.user.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisherImpl implements EventPublisher {

    private final StreamBridge streamBridge;

    @Override
    public void processEmail(final NotificationEvent event) {
        boolean sent = streamBridge.send("processEmail-out-0", event);
        log.info("Email event publishing result: {} | {}", sent, event.eventId());
        if (!sent) {
            throw new IllegalStateException("Unable to publish email notification");
        }
    }
}
