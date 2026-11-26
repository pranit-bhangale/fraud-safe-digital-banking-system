package com.banking.helpdesk.cache.service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

public interface Cache<K, V> {

    Optional<V> get(K key);

    void put(K key, V value);

    void put(K key, V value, Duration ttl);

    V computeIfAbsent(K key, Function<K, V> mappingFunction);

    void invalidate(K key);

    void clear();
}
