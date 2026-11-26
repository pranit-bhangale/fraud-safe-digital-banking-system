package com.banking.helpdesk.cache.spring;

import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Spring CacheManager implementation providing AtomicReferenceSpringCache instances.
 */
public class AtomicReferenceCacheManager implements CacheManager {

    private final ConcurrentMap<String, AtomicReferenceSpringCache> cacheMap = new ConcurrentHashMap<>();
    private final int defaultMaxCapacity;
    private final Duration defaultTtl;

    public AtomicReferenceCacheManager(int defaultMaxCapacity, Duration defaultTtl) {
        this.defaultMaxCapacity = defaultMaxCapacity;
        this.defaultTtl = defaultTtl;
    }

    @Override
    public Cache getCache(@NonNull String name) {
        return this.cacheMap.computeIfAbsent(name,
                n -> new AtomicReferenceSpringCache(n, defaultMaxCapacity, defaultTtl));
    }

    @Override
    public @NonNull Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }
}
