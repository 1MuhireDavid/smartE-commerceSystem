package org.ecommerce.api.service.impl;

import org.ecommerce.api.service.RateLimitService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding-window rate limiter — Epic 3 (US 3.1).
 *
 * Concurrent data structures used:
 *   ConcurrentHashMap<String, Bucket>  — per-key request counter; ConcurrentHashMap.compute()
 *     atomically resets the window or returns the existing bucket without an explicit lock.
 *   CopyOnWriteArrayList<String>       — violation audit trail; iterated frequently on admin
 *     dashboards, appended rarely (only on limit exceeded), making COWAL the right fit.
 *   AtomicInteger (inside Bucket)      — lock-free increment for the per-window request count.
 *
 * No explicit synchronized blocks are needed: fine-grained stripe locking inside
 * ConcurrentHashMap plus lock-free AtomicInteger cover every shared-state mutation.
 */
@Service
public class RateLimitServiceImpl implements RateLimitService {

    public static final int  MAX_REQUESTS = 10;
    public static final long WINDOW_MS    = 60_000L;   // 1 minute

    private static final class Bucket {
        final AtomicInteger count = new AtomicInteger(0);
        final long windowStart;
        Bucket(long ts) { this.windowStart = ts; }
    }

    private final ConcurrentHashMap<String, Bucket> buckets    = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String>       violations = new CopyOnWriteArrayList<>();

    @Override
    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();

        // compute() holds the bucket's hash-stripe lock: window reset + bucket selection are atomic.
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                return new Bucket(now);   // new window, counter starts at 0
            }
            return existing;
        });

        int count = bucket.count.incrementAndGet();
        if (count > MAX_REQUESTS) {
            violations.add(key + "@" + Instant.now());
            return false;
        }
        return true;
    }

    @Override
    public int getRemainingRequests(String key) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.get(key);
        if (bucket == null || now - bucket.windowStart >= WINDOW_MS) {
            return MAX_REQUESTS;
        }
        return Math.max(0, MAX_REQUESTS - bucket.count.get());
    }

    @Override
    public List<String> getRecentViolations() {
        return Collections.unmodifiableList(violations);
    }
}
