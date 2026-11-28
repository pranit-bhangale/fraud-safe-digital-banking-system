package com.banking.payment.webhook;

import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.entity.WebhookDelivery;
import com.banking.payment.entity.WebhookSubscription;
import com.banking.payment.model.WebhookDeliveryStatus;
import com.banking.payment.repository.WebhookDeliveryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final HmacSignatureUtil hmacSignatureUtil;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Async
    public void dispatch(WebhookSubscription subscription, WebhookPayload payload) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload: {}", e.getMessage(), e);
            return;
        }

        String signature = hmacSignatureUtil.generateSignature(jsonPayload, subscription.getSecretKey());

        WebhookDelivery delivery = WebhookDelivery.builder()
                .subscriptionId(subscription.getId())
                .eventId(payload.eventId())
                .eventType(payload.eventType())
                .payload(jsonPayload)
                .signatureHeader("v1=" + signature)
                .deliveryStatus(WebhookDeliveryStatus.PENDING)
                .attemptCount(1)
                .build();
        delivery = deliveryRepository.save(delivery);

        log.info("Dispatching webhook: event={}, targetUrl={}, eventId={}",
                payload.eventType(), subscription.getTargetUrl(), payload.eventId());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getTargetUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Banking-Signature", "v1=" + signature)
                    .header("X-Banking-Event", payload.eventType())
                    .header("X-Banking-Event-Id", payload.eventId())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            delivery.setHttpStatusCode(response.statusCode());
            delivery.setResponseBody(response.body() != null && response.body().length() > 500
                    ? response.body().substring(0, 500)
                    : response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.setDeliveryStatus(WebhookDeliveryStatus.DELIVERED);
                delivery.setDeliveredAt(Instant.now());
                log.info("Webhook successfully delivered: subscriptionId={}, status={}",
                        subscription.getId(), response.statusCode());
            } else {
                delivery.setDeliveryStatus(WebhookDeliveryStatus.RETRYING);
                delivery.setNextRetryAt(Instant.now().plusSeconds(30));
                log.warn("Webhook target returned non-2xx: status={}, scheduling retry", response.statusCode());
            }
        } catch (Exception ex) {
            log.error("Webhook network delivery failed: targetUrl={}, error={}",
                    subscription.getTargetUrl(), ex.getMessage());
            delivery.setHttpStatusCode(500);
            delivery.setResponseBody("Delivery exception: " + ex.getMessage());
            delivery.setDeliveryStatus(WebhookDeliveryStatus.RETRYING);
            delivery.setNextRetryAt(Instant.now().plusSeconds(30));
        }

        deliveryRepository.save(delivery);
    }
}
