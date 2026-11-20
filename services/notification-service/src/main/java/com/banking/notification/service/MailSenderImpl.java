package com.banking.notification.service;

import com.banking.notification.dto.EmailRequest;
import com.banking.notification.exception.EmailDeliveryException;
import com.banking.notification.exception.EmailPermanentDeliveryException;
import com.banking.notification.exception.EmailRenderException;
import com.banking.notification.exception.EmailValidationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailSenderImpl implements MailSender {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.from.address}")
    private String fromAddress;

    @Value("${mail.from.name}")
    private String fromName;

    @Override
    public void send(final EmailRequest request, final String templateName) {
        final String eventId = request.eventId();
        log.info("Sending email eventId: {} purpose: {} template: {}", eventId, request.type(), templateName);
        final String html = render(request, templateName);
        final MimeMessage message = build(request, html);
        deliver(request, message);
        log.info("Email accepted by SMTP server eventId: {} to: {}", eventId, request.recipient().to());
    }

    private String render(final EmailRequest request, final String templateName) {
        try {
            final Context context = new Context(Locale.ENGLISH);
            context.setVariables(request.templateData());
            context.setVariable("subject", request.subject());
            context.setVariable("recipientEmail", request.recipient().to());
            return this.templateEngine.process(templateName, context);
        } catch (final TemplateEngineException e) {
            throw new EmailRenderException
                    ("Failed to render template '" + templateName + "'", request.eventId(), e);
        }
    }

    private MimeMessage build(final EmailRequest request, final String html) {
        try {
            final MimeMessage message = this.javaMailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(this.fromAddress, this.fromName);
            helper.setTo(request.recipient().to());
            helper.setSubject(request.subject());
            helper.setText(html, true);
            message.setHeader("X-Event-Id", request.eventId());
            return message;
        } catch (final MessagingException | UnsupportedEncodingException e) {
            throw new EmailValidationException
                    ("Could not build email (check recipient address)", request.eventId(), e);
        }
    }

    private void deliver(final EmailRequest request, final MimeMessage message) {
        try {
            this.javaMailSender.send(message);
        } catch (final MailAuthenticationException | MailParseException | MailPreparationException e) {
            log.error("SMTP rejected email eventId={} (non-retryable)", request.eventId(), e);
            throw new EmailPermanentDeliveryException("SMTP rejected email", request.eventId(), e);
        } catch (final MailException e) {
            log.warn("SMTP send failed eventId={} (will retry)", request.eventId(), e);
            throw new EmailDeliveryException("SMTP send failed", request.eventId(), true, e);
        }
    }
}