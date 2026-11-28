package com.banking.payment.repository;

import com.banking.payment.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {

    List<WebhookSubscription> findByMerchantIdAndIsActiveTrue(UUID merchantId);

    List<WebhookSubscription> findByIsActiveTrue();
}
