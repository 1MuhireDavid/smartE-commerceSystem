package org.example.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic TTL-based in-memory cache backed by a ConcurrentHashMap.
 *
 * <p>Each entry carries an expiry timestamp. Reads that hit an expired entry
 * treat it as a miss and evict it lazily. Writes replace any existing value
 * and reset the TTL.  All operations are thread-safe through the underlying
 * ConcurrentHashMap — no additional locking is required for reads.
 *
 * @param <K> cache key type
 * @param <V> cached value type
 */
public class InMemoryCache<K, V> {

    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L; // 5 minutes

    private final Map<K, Entry<V>> store = new ConcurrentHashMap<>();
    private final long ttlMs;

    public InMemoryCache() {
        this(DEFAULT_TTL_MS);
    }

    public InMemoryCache(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    // ── Core API ──────────────────────────────────────────────────────────────

    public void put(K key, V value) {
        store.put(key, new Entry<>(value, System.currentTimeMillis() + ttlMs));
    }

    public V get(K key) {
        Entry<V> entry = store.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public void invalidate(K key) {
        store.remove(key);
    }

    /** Invalidate all entries whose key matches a given prefix (String keys only). */
    @SuppressWarnings("unchecked")
    public void invalidateByPrefix(String prefix) {
        List<K> toRemove = new ArrayList<>();
        for (K key : store.keySet()) {
            if (key instanceof String s && s.startsWith(prefix)) {
                toRemove.add(key);
            }
        }
        toRemove.forEach(store::remove);
    }

    /** Remove all entries from the cache. */
    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }

    // ── Internal entry ────────────────────────────────────────────────────────

    private record Entry<V>(V value, long expiresAt) {}
}
