package com.banking.payment.controller;

import com.banking.payment.dto.CreatePaymentOrderRequest;
import com.banking.payment.dto.PaymentOrderResponse;
import com.banking.payment.service.PaymentService;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        log.info("Received request to create payment order for merchant={}", request.merchantId());
        PaymentOrderResponse response = paymentService.createPaymentOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderReference}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentOrderResponse> getPaymentOrder(@PathVariable String orderReference) {
        return ResponseEntity.ok(paymentService.getPaymentOrder(orderReference));
    }

    @PostMapping("/{orderReference}/pay")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentOrderResponse> processPayment(@PathVariable String orderReference) {
        log.info("Processing checkout payment for orderReference={}", orderReference);
        PaymentOrderResponse response = paymentService.processPayment(orderReference);
        return ResponseEntity.ok(response);
    }
}
