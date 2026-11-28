package com.banking.payment.service;

import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.dto.WebhookSubscriptionRequest;
import com.banking.payment.dto.WebhookSubscriptionResponse;
import com.banking.payment.entity.WebhookDelivery;
import com.banking.payment.entity.WebhookSubscription;
import com.banking.payment.repository.WebhookDeliveryRepository;
import com.banking.payment.repository.WebhookSubscriptionRepository;
import com.banking.payment.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher webhookDispatcher;

    @Override
    @Transactional
    public WebhookSubscriptionResponse registerSubscription(WebhookSubscriptionRequest request) {
        log.info("Registering webhook subscription: merchantId={}, targetUrl={}",
                request.merchantId(), request.targetUrl());

        WebhookSubscription subscription = WebhookSubscription.builder()
                .merchantId(request.merchantId())
                .targetUrl(request.targetUrl())
                .secretKey(request.secretKey())
                .eventTypes(request.eventTypes())
                .isActive(true)
                .build();

        subscription = subscriptionRepository.save(subscription);

        return WebhookSubscriptionResponse.builder()
                .id(subscription.getId())
                .merchantId(subscription.getMerchantId())
                .targetUrl(subscription.getTargetUrl())
                .eventTypes(subscription.getEventTypes())
                .isActive(subscription.isActive())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookSubscriptionResponse> getSubscriptionsByMerchant(UUID merchantId) {
        return subscriptionRepository.findByMerchantIdAndIsActiveTrue(merchantId).stream()
                .map(s -> WebhookSubscriptionResponse.builder()
                        .id(s.getId())
                        .merchantId(s.getMerchantId())
                        .targetUrl(s.getTargetUrl())
                        .eventTypes(s.getEventTypes())
                        .isActive(s.isActive())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public void triggerEvent(UUID merchantId, WebhookPayload payload) {
        List<WebhookSubscription> subscriptions = subscriptionRepository.findByMerchantIdAndIsActiveTrue(merchantId);
        if (subscriptions.isEmpty()) {
            log.info("No active webhook subscriptions found for merchantId={}", merchantId);
            return;
        }

        for (WebhookSubscription sub : subscriptions) {
            if (sub.getEventTypes().contains(payload.eventType()) || sub.getEventTypes().contains("*")) {
                webhookDispatcher.dispatch(sub, payload);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDelivery> getDeliveryHistory(UUID subscriptionId) {
        return deliveryRepository.findBySubscriptionId(subscriptionId);
    }
}
