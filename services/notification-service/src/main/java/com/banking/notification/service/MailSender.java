package com.banking.notification.service;

import com.banking.notification.dto.EmailRequest;

@FunctionalInterface
public interface MailSender {

    void send(EmailRequest request, String templateName);
}

