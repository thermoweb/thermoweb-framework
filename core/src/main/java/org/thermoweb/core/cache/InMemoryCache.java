package org.thermoweb.core.cache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InMemoryCache<K, V> implements GenericCache<K, V> {

    public static final Long DEFAULT_CACHE_TIMEOUT = 6000L;
    //FIXME: use concurrent hash map ?
    private final LinkedHashMap<K, CacheValue<V>> cache;
    private final Long cacheTimeout;

    public InMemoryCache() {
        this(DEFAULT_CACHE_TIMEOUT);
    }

    public InMemoryCache(Long timeout) {
        this.cacheTimeout = timeout;
        this.cache = new LinkedHashMap<>();
    }

    @Override
    public void clean() {
        for (K key : this.getExpiredKeys()) {
            this.remove(key);
        }
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public Optional<V> get(K key) {
        this.clean();
        return Optional.ofNullable(this.cache.get(key)).map(CacheValue::getValue);
    }

    @Override
    public void put(K key, V value) {
        this.cache.put(key, createCacheValue(value));
    }

    @Override
    public void remove(K key) {
        this.cache.remove(key);
    }

    private Set<K> getExpiredKeys() {
        return this.cache.keySet().parallelStream().filter(this::isExpired).collect(Collectors.toSet());
    }

    private boolean isExpired(K key) {
        Instant expirationInstant = cache.get(key).getCreatedAt().plus(cacheTimeout, ChronoUnit.MILLIS);
        return Instant.now().isAfter(expirationInstant);
    }

    private CacheValue<V> createCacheValue(V value) {
        Instant now = Instant.now();
        return new CacheValue<>() {
            @Override
            public V getValue() {
                return value;
            }

            @Override
            public Instant getCreatedAt() {
                return now;
            }
        };
    }

    interface CacheValue<V> {
        V getValue();

        Instant getCreatedAt();
    }
}
