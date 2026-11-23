package com.banking.notification.repository;

import com.banking.notification.entity.NotificationAudit;
import com.banking.notification.entity.NotificationChannel;
import com.banking.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, UUID> {

    Optional<AuditState> findStateByEventIdAndChannel(String eventId, NotificationChannel channel);

    @Modifying
    @Query("""
            update NotificationAudit a
               set a.status = :status,
                   a.attempts = a.attempts + 1,
                   a.errorMessage = null,
                   a.updatedAt = :now,
                   a.version = a.version + 1
             where a.id = :id
            """)
    int markProcessing(@Param("id") UUID id,
                       @Param("status") NotificationStatus status,
                       @Param("now") Instant now);

    @Modifying
    @Query("""
            update NotificationAudit a
               set a.status = :status,
                   a.errorMessage = null,
                   a.updatedAt = :now,
                   a.version = a.version + 1
             where a.id = :id
            """)
    int markSent(
            @Param("id") UUID id,
            @Param("status") NotificationStatus status,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            update NotificationAudit a
               set a.status = :status,
                   a.errorMessage = :error,
                   a.updatedAt = :now,
                   a.version = a.version + 1
             where a.id = :id
            """)
    int markFailed(
            @Param("id") UUID id,
            @Param("status") NotificationStatus status,
            @Param("error") String error,
            @Param("now") Instant now
    );

    interface AuditState {
        UUID getId();

        NotificationStatus getStatus();
    }
}