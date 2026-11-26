package com.banking.payment.service;

import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.dto.WebhookSubscriptionRequest;
import com.banking.payment.dto.WebhookSubscriptionResponse;
import com.banking.payment.entity.WebhookDelivery;
import com.banking.payment.entity.WebhookSubscription;

import java.util.List;
import java.util.UUID;

public interface WebhookService {

    WebhookSubscriptionResponse registerSubscription(WebhookSubscriptionRequest request);

    List<WebhookSubscriptionResponse> getSubscriptionsByMerchant(UUID merchantId);

    void triggerEvent(UUID merchantId, WebhookPayload payload);

    List<WebhookDelivery> getDeliveryHistory(UUID subscriptionId);
}
