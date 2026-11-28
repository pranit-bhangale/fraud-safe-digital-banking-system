package com.banking.payment.controller;

import com.banking.payment.dto.CreatePaymentOrderRequest;
import com.banking.payment.dto.PaymentOrderResponse;
import com.banking.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payment Orders & Checkout", description = "Endpoints for merchant order creation, order status queries, and customer payment execution")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create Payment Order", description = "Creates a new merchant payment order with an idempotent checkout reference.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment order created successfully", content = @Content(schema = @Schema(implementation = PaymentOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content)
    })
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        log.info("Received request to create payment order for merchant={}", request.merchantId());
        PaymentOrderResponse response = paymentService.createPaymentOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{orderReference}", version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Payment Order", description = "Fetches payment order details and current settlement status by order reference.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment order found", content = @Content(schema = @Schema(implementation = PaymentOrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment order reference not found", content = @Content)
    })
    public ResponseEntity<PaymentOrderResponse> getPaymentOrder(
            @Parameter(description = "Unique order reference (e.g. ORD-1732737600000-ABCDEF12)", required = true)
            @PathVariable String orderReference) {
        return ResponseEntity.ok(paymentService.getPaymentOrder(orderReference));
    }

    @PostMapping(value = "/{orderReference}/pay", version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Process Payment Checkout", description = "Settles a payment order, marks it COMPLETED, prints verification details, and triggers merchant webhooks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment successfully processed", content = @Content(schema = @Schema(implementation = PaymentOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Order already completed or invalid state", content = @Content)
    })
    public ResponseEntity<PaymentOrderResponse> processPayment(
            @Parameter(description = "Unique order reference to settle", required = true)
            @PathVariable String orderReference) {
        log.info("Processing checkout payment for orderReference={}", orderReference);
        PaymentOrderResponse response = paymentService.processPayment(orderReference);
        return ResponseEntity.ok(response);
    }
}
