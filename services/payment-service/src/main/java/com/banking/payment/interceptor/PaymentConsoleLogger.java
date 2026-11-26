package com.banking.payment.interceptor;

import com.banking.payment.entity.PaymentOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Concrete Console Diagnostic Logger:
 * Explicitly prints payment completion to standard console BEFORE proceeding to webhook dispatch.
 */
@Slf4j
@Component
public class PaymentConsoleLogger {

    public void logPaymentDoneToConsole(PaymentOrder order, String eventType) {
        String border = "=".repeat(80);
        System.out.println(border);
        System.out.println(" >>> [PAYMENT NOTIFICATION CONSOLE INTERCEPTOR] <<<");
        System.out.println(" Event Type       : " + eventType);
        System.out.println(" Timestamp        : " + Instant.now());
        System.out.println(" Order Reference  : " + order.getOrderReference());
        System.out.println(" Merchant ID      : " + order.getMerchantId());
        System.out.println(" Customer Account : " + order.getCustomerAccountNo());
        System.out.println(" Amount           : " + order.getCurrency() + " " + order.getAmount());
        System.out.println(" Payment Status   : " + order.getStatus());
        System.out.println(" Transaction ID   : " + (order.getTransactionId() != null ? order.getTransactionId() : "N/A"));
        System.out.println(" >>> Initial payment confirmation printed to console.");
        System.out.println(" >>> Handing off to Webhook Engine for cryptographic dispatch...");
        System.out.println(border);

        log.info("Payment console diagnostic completed for orderRef={}, status={}. Proceeding with webhook dispatch.",
                order.getOrderReference(), order.getStatus());
    }
}
