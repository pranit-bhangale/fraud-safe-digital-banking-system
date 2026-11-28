package com.banking.payment.controller;

import com.banking.payment.dto.WebhookSubscriptionRequest;
import com.banking.payment.dto.WebhookSubscriptionResponse;
import com.banking.payment.entity.WebhookDelivery;
import com.banking.payment.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payment Webhooks", description = "Endpoints for registering merchant webhook target URLs and reviewing event dispatch deliveries")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping(value = "/subscriptions", version = "v1")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Register Webhook Subscription", description = "Registers an HTTP POST target URL and shared secret key for automated event notifications.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Webhook subscription registered", content = @Content(schema = @Schema(implementation = WebhookSubscriptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid subscription request", content = @Content)
    })
    public ResponseEntity<WebhookSubscriptionResponse> registerSubscription(
            @Valid @RequestBody WebhookSubscriptionRequest request) {
        log.info("Registering webhook endpoint for merchant={}", request.merchantId());
        WebhookSubscriptionResponse response = webhookService.registerSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/subscriptions/merchant/{merchantId}", version = "v1")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get Subscriptions by Merchant", description = "Lists all active webhook subscriptions registered under a merchant ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active subscriptions found", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebhookSubscriptionResponse.class))))
    })
    public ResponseEntity<List<WebhookSubscriptionResponse>> getSubscriptions(
            @Parameter(description = "Merchant unique UUID identifier", required = true)
            @PathVariable UUID merchantId) {
        return ResponseEntity.ok(webhookService.getSubscriptionsByMerchant(merchantId));
    }

    @GetMapping(value = "/subscriptions/{subscriptionId}/deliveries", version = "v1")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get Delivery History", description = "Retrieves the history of webhook HTTP dispatch attempts, status codes, and latencies.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery audit history found", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebhookDelivery.class))))
    })
    public ResponseEntity<List<WebhookDelivery>> getDeliveries(
            @Parameter(description = "Webhook subscription UUID", required = true)
            @PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(webhookService.getDeliveryHistory(subscriptionId));
    }
}
