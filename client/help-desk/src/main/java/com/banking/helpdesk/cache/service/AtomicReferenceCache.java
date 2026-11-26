package com.pranit.praya.cache.service;

import com.pranit.praya.cache.model.CacheEntry;
import com.pranit.praya.cache.model.CacheSnapshot;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Thread-safe, lock-free cache using AtomicReference CAS loop.
 */
public class AtomicReferenceCache<K, V> implements Cache<K, V> {

    private final AtomicReference<CacheSnapshot<K, V>> snapshotRef;
    private final int maxCapacity;
    private final Duration defaultTtl;

    public AtomicReferenceCache(int maxCapacity, Duration defaultTtl) {
        this.maxCapacity = Math.max(1, maxCapacity);
        this.defaultTtl = defaultTtl;
        this.snapshotRef = new AtomicReference<>(CacheSnapshot.empty());
    }

    @Override
    public Optional<V> get(@NonNull K key) {
        Objects.requireNonNull(key, "key cannot be null");
        CacheEntry<V> entry = this.snapshotRef.get().entries().get(key);
        if (entry == null || entry.isExpired()) return Optional.empty();
        return Optional.of(entry.value());
    }

    @Override
    public void put(@NonNull K key, @NonNull V value) {
        put(key, value, this.defaultTtl);
    }

    @Override
    public void put(@NonNull K key, @NonNull V value, Duration ttl) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        CacheEntry<V> entry = CacheEntry.<V>builder()
                .value(value)
                .createdAt(Instant.now())
                .ttl(ttl)
                .build();
        this.snapshotRef.updateAndGet(s -> s.withPut(key, entry, this.maxCapacity));
    }

    @Override
    public V computeIfAbsent(@NonNull K key, @NonNull Function<K, V> mappingFunction) {
        return get(key).orElseGet(() -> {
            V computed = mappingFunction.apply(key);
            put(key, computed);
            return computed;
        });
    }

    @Override
    public void invalidate(@NonNull K key) {
        snapshotRef.updateAndGet(s -> s.withRemove(key));
    }

    @Override
    public void clear() {
        snapshotRef.set(CacheSnapshot.empty());
    }
}
