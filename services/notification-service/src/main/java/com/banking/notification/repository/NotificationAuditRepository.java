package com.banking.notification.repository;

import com.banking.notification.entity.NotificationAudit;
import com.banking.notification.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, UUID> {

    Optional<NotificationAudit> findByEventIdAndChannel(String eventId, NotificationChannel channel);
}