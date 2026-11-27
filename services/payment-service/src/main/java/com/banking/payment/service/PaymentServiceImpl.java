package com.banking.payment.service;

import com.banking.payment.dto.CreatePaymentOrderRequest;
import com.banking.payment.dto.PaymentOrderResponse;
import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.entity.PaymentOrder;
import com.banking.payment.interceptor.PaymentConsoleLogger;
import com.banking.payment.model.PaymentStatus;
import com.banking.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository orderRepository;
    private final PaymentConsoleLogger consoleLogger;
    private final WebhookService webhookService;

    @Override
    @Transactional
    public PaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest request) {
        log.info("Creating payment order: merchantId={}, amount={}, customerAccount={}",
                request.merchantId(), request.amount(), request.customerAccountNo());

        // Idempotency check
        var existing = orderRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent replay for payment order: key={}, orderRef={}",
                    request.idempotencyKey(), existing.get().getOrderReference());
            return mapToResponse(existing.get());
        }

        String orderReference = "ORD-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentOrder order = PaymentOrder.builder()
                .orderReference(orderReference)
                .merchantId(request.merchantId())
                .customerAccountNo(request.customerAccountNo())
                .amount(request.amount())
                .currency(request.currency())
                .status(PaymentStatus.CREATED)
                .idempotencyKey(request.idempotencyKey())
                .build();

        order = orderRepository.save(order);
        log.info("Payment order created: orderRef={}", order.getOrderReference());
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOrderResponse getPaymentOrder(String orderReference) {
        PaymentOrder order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderReference));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public PaymentOrderResponse processPayment(String orderReference) {
        PaymentOrder order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderReference));

        if (order.getStatus() == PaymentStatus.COMPLETED) {
            log.warn("Payment order already completed: {}", orderReference);
            return mapToResponse(order);
        }

        log.info("Processing payment order: orderRef={}, amount={}", orderReference, order.getAmount());
        order.setStatus(PaymentStatus.PROCESSING);
        order.setUpdatedAt(Instant.now());
        order = orderRepository.save(order);

        // Simulate successful transaction settlement and complete payment
        UUID transactionId = UUID.randomUUID();
        return completePayment(orderReference, transactionId);
    }

    @Override
    @Transactional
    public PaymentOrderResponse completePayment(String orderReference, UUID transactionId) {
        PaymentOrder order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderReference));

        order.setStatus(PaymentStatus.COMPLETED);
        order.setTransactionId(transactionId);
        order.setUpdatedAt(Instant.now());
        order = orderRepository.save(order);

        // STEP 1: Explicitly print payment completion details to console first!
        consoleLogger.logPaymentDoneToConsole(order, "payment.completed");

        // STEP 2: Proceed with webhook delivery!
        WebhookPayload payload = WebhookPayload.builder()
                .eventId("evt_" + UUID.randomUUID().toString().replace("-", ""))
                .eventType("payment.completed")
                .timestamp(Instant.now())
                .data(Map.of(
                        "orderReference", order.getOrderReference(),
                        "merchantId", order.getMerchantId().toString(),
                        "customerAccountNo", order.getCustomerAccountNo(),
                        "amount", order.getAmount(),
                        "currency", order.getCurrency(),
                        "status", "COMPLETED",
                        "transactionId", transactionId.toString(),
                        "completedAt", Instant.now().toString()
                ))
                .build();

        //webhookService.triggerEvent(order.getMerchantId(), payload);

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public PaymentOrderResponse failPayment(String orderReference, String reason) {
        PaymentOrder order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderReference));

        order.setStatus(PaymentStatus.FAILED);
        order.setFailureReason(reason);
        order.setUpdatedAt(Instant.now());
        order = orderRepository.save(order);

        // Print failure to console
        consoleLogger.logPaymentDoneToConsole(order, "payment.failed");

        // Trigger failure webhook
        WebhookPayload payload = WebhookPayload.builder()
                .eventId("evt_" + UUID.randomUUID().toString().replace("-", ""))
                .eventType("payment.failed")
                .timestamp(Instant.now())
                .data(Map.of(
                        "orderReference", order.getOrderReference(),
                        "merchantId", order.getMerchantId().toString(),
                        "amount", order.getAmount(),
                        "status", "FAILED",
                        "reason", reason != null ? reason : "Payment execution failed"
                ))
                .build();

        //webhookService.triggerEvent(order.getMerchantId(), payload);

        return mapToResponse(order);
    }

    private PaymentOrderResponse mapToResponse(PaymentOrder order) {
        return PaymentOrderResponse.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .merchantId(order.getMerchantId())
                .customerAccountNo(order.getCustomerAccountNo())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .transactionId(order.getTransactionId())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
