package com.banking.user.publisher.impl;

import com.banking.user.event.NotificationEvent;
import com.banking.user.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisherImpl implements EventPublisher {

    private final StreamBridge streamBridge;

    @Override
    public void processEmail(final NotificationEvent event) {
        this.streamBridge.send(" processEmail-out-0", event);
    }
}
