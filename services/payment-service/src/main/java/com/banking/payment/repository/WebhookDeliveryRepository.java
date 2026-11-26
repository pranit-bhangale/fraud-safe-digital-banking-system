package com.banking.payment.repository;

import com.banking.payment.entity.WebhookDelivery;
import com.banking.payment.model.WebhookDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    List<WebhookDelivery> findBySubscriptionId(UUID subscriptionId);

    List<WebhookDelivery> findByDeliveryStatusAndNextRetryAtBefore(WebhookDeliveryStatus status, Instant time);
}
