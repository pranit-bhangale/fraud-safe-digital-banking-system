package com.banking.payment.service;

import com.banking.payment.dto.CreatePaymentOrderRequest;
import com.banking.payment.dto.PaymentOrderResponse;

import java.util.UUID;

public interface PaymentService {

    PaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest request);

    PaymentOrderResponse getPaymentOrder(String orderReference);

    PaymentOrderResponse processPayment(String orderReference);

    PaymentOrderResponse completePayment(String orderReference, UUID transactionId);

    PaymentOrderResponse failPayment(String orderReference, String reason);
}
