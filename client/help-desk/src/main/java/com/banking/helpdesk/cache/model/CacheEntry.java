package com.banking.helpdesk.cache.model;

import lombok.Builder;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable record representing an entry in the cache.
 */
@Builder
public record CacheEntry<V>(
        V value,
        Instant createdAt,
        Duration ttl
) {
    public boolean isExpired() {
        return ttl != null && Instant.now().isAfter(createdAt.plus(ttl));
    }
}
