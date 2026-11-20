package com.banking.user.store;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Thread-safe, in-memory token blacklist with TTL.
 */
@Slf4j
@Component
public class TokenBlacklist {

    private static final Duration MAX_TTL = Duration.ofDays(365);
    private static final long CLEANUP_INTERVAL_SECONDS = 60;

    private final ConcurrentMap<String, Long> tokens = new ConcurrentHashMap<>();

    private final LongSupplier nanoClock;

    private final ScheduledExecutorService cleaner =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                final Thread thread = new Thread(runnable, "token-blacklist-cleaner");
                thread.setDaemon(true);
                return thread;
            });

    public TokenBlacklist() {
        this(System::nanoTime);
    }

    TokenBlacklist(final LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    private static boolean hasExpired(final long expiresAt, final long now) {
        return now - expiresAt >= 0;
    }

    @PostConstruct
    void startCleaner() {
        this.cleaner.scheduleWithFixedDelay
                (this::evictExpired, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopCleaner() {
        this.cleaner.shutdownNow();
    }

    public void blacklist(final String token, final Duration ttl) {
        if (token == null || token.isBlank()
                || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Invalid token or TTL");
        }
        final Duration effectiveTtl = ttl.compareTo(MAX_TTL) > 0 ? MAX_TTL : ttl;
        final long expiresAt = this.nanoClock.getAsLong() + effectiveTtl.toNanos();
        this.tokens.merge(token, expiresAt, (existing, candidate) -> candidate - existing > 0 ? candidate : existing);
    }

    public boolean isBlacklisted(final String token) {
        if (token == null) return false;
        final Long expiresAt = this.tokens.get(token);
        if (expiresAt == null) return false;
        if (hasExpired(expiresAt, this.nanoClock.getAsLong())) {
            this.tokens.remove(token, expiresAt);
            return false;
        }
        return true;
    }

    public void remove(final String token) {
        if (token != null) {
            this.tokens.remove(token);
        }
    }

    public int size() {
        return this.tokens.size();
    }

    void evictExpired() {
        try {
            final long now = this.nanoClock.getAsLong();
            this.tokens.values().removeIf(expiresAt -> hasExpired(expiresAt, now));
        } catch (final RuntimeException e) {
            log.error("Token blacklist cleanup failed", e);
        }
    }
}