package com.pranit.praya.cache.spring;

import com.pranit.praya.cache.service.AtomicReferenceCache;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Spring Cache abstraction adapter backed by thread-safe, lock-free AtomicReferenceCache.
 * Supports @Cacheable, @CachePut, and @CacheEvict.
 */
public class AtomicReferenceSpringCache implements Cache {

    private final String name;
    private final AtomicReferenceCache<Object, Object> nativeCache;

    public AtomicReferenceSpringCache(String name, int maxCapacity, Duration defaultTtl) {
        this.name = Objects.requireNonNull(name, "Cache name must not be null");
        this.nativeCache = new AtomicReferenceCache<>(maxCapacity, defaultTtl);
    }

    @Override
    public @NonNull String getName() {
        return this.name;
    }

    @Override
    public @NonNull AtomicReferenceCache<Object, Object> getNativeCache() {
        return this.nativeCache;
    }

    @Override
    public @Nullable ValueWrapper get(@NonNull Object key) {
        return this.nativeCache.get(key)
                .map(SimpleValueWrapper::new)
                .orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(@NonNull Object key, @Nullable Class<T> type) {
        return (T) this.nativeCache.get(key)
                .filter(val -> type == null || type.isInstance(val))
                .orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        return (T) this.nativeCache.computeIfAbsent(key, _ -> {
            try {
                return valueLoader.call();
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        });
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        if (value != null) {
            this.nativeCache.put(key, value);
        } else {
            this.nativeCache.invalidate(key);
        }
    }

    @Override
    public @Nullable ValueWrapper putIfAbsent(@NonNull Object key, @Nullable Object value) {
        var existing = get(key);
        if (existing == null && value != null) {
            put(key, value);
            return null;
        }
        return existing;
    }

    @Override
    public void evict(@NonNull Object key) {
        this.nativeCache.invalidate(key);
    }

    @Override
    public void clear() {
        this.nativeCache.clear();
    }
}
