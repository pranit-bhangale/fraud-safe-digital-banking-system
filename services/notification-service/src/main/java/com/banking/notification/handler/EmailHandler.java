package com.banking.notification.handler;


import com.banking.notification.dto.EmailRequest;
import com.banking.notification.entity.NotificationType;

public interface EmailHandler {

    NotificationType supports();

    void send(EmailRequest request);
}