package com.banking.payment.controller;

import com.banking.payment.dto.WebhookSubscriptionRequest;
import com.banking.payment.dto.WebhookSubscriptionResponse;
import com.banking.payment.entity.WebhookDelivery;
import com.banking.payment.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Validated
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/subscriptions")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<WebhookSubscriptionResponse> registerSubscription(
            @Valid @RequestBody WebhookSubscriptionRequest request) {
        log.info("Registering webhook endpoint for merchant={}", request.merchantId());
        WebhookSubscriptionResponse response = webhookService.registerSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/subscriptions/merchant/{merchantId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WebhookSubscriptionResponse>> getSubscriptions(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(webhookService.getSubscriptionsByMerchant(merchantId));
    }

    @GetMapping("/subscriptions/{subscriptionId}/deliveries")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WebhookDelivery>> getDeliveries(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(webhookService.getDeliveryHistory(subscriptionId));
    }
}
