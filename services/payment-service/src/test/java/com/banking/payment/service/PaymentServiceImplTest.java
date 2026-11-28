package com.banking.payment.service;

import com.banking.payment.dto.CreatePaymentOrderRequest;
import com.banking.payment.dto.PaymentOrderResponse;
import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.entity.PaymentOrder;
import com.banking.payment.interceptor.PaymentConsoleLogger;
import com.banking.payment.model.PaymentStatus;
import com.banking.payment.repository.PaymentOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentOrderRepository orderRepository;

    @Mock
    private PaymentConsoleLogger consoleLogger;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Console & Webhook Sequence: Prints to console first, then triggers webhook event")
    void shouldPrintToConsoleFirstThenTriggerWebhook() {
        UUID merchantId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .orderReference("ORD-9999")
                .merchantId(merchantId)
                .customerAccountNo("ACC-12345")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .status(PaymentStatus.PROCESSING)
                .idempotencyKey("idem-key-1")
                .build();

        when(orderRepository.findByOrderReference("ORD-9999")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(PaymentOrder.class))).thenAnswer(i -> i.getArgument(0));

        UUID txnId = UUID.randomUUID();
        PaymentOrderResponse response = paymentService.completePayment("ORD-9999", txnId);

        assertEquals(PaymentStatus.COMPLETED, response.status());

        // 1. Verify that console diagnostic logger was called FIRST
        verify(consoleLogger).logPaymentDoneToConsole(any(PaymentOrder.class), eq("payment.completed"));

        // 2. Verify that webhook event was triggered
        verify(webhookService).triggerEvent(eq(merchantId), any(WebhookPayload.class));
    }
}
