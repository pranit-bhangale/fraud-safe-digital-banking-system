package com.banking.notification.router;


import com.banking.notification.entity.NotificationType;
import com.banking.notification.event.EmailEvent;

@FunctionalInterface
public interface EmailRouter {

    void send(EmailEvent event, NotificationType type);
}

