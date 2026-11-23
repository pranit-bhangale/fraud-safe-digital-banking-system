package com.banking.user.publisher;

import com.banking.user.event.NotificationEvent;

public interface EventPublisher {
    
    void processEmail(NotificationEvent event);
}
