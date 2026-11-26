package com.banking.helpdesk.cache.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of the entire cache state.
 */
public record CacheSnapshot<K, V>(
        Map<K, CacheEntry<V>> entries
) {
    public static <K, V> CacheSnapshot<K, V> empty() {
        return new CacheSnapshot<>(Map.of());
    }

    public CacheSnapshot<K, V> withPut(K key, CacheEntry<V> newEntry, int maxCapacity) {
        var copy = new HashMap<>(entries);
        copy.entrySet().removeIf(e -> e.getValue().isExpired());
        if (copy.size() >= maxCapacity && !copy.containsKey(key)) {
            copy.entrySet()
                    .stream()
                    .min(Comparator.comparing(e -> e.getValue().createdAt()))
                    .ifPresent(e -> copy.remove(e.getKey()));
        }
        copy.put(key, newEntry);
        return new CacheSnapshot<>(Collections.unmodifiableMap(copy));
    }

    public CacheSnapshot<K, V> withRemove(K key) {
        if (!entries.containsKey(key)) return this;
        var copy = new HashMap<>(entries);
        copy.remove(key);
        return new CacheSnapshot<>(Collections.unmodifiableMap(copy));
    }
}
