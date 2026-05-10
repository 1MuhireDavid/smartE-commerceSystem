package org.ecommerce.api.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * In-memory token revocation store — Epic 3 (US 3.1).
 *
 * Data structures:
 *   ConcurrentHashMap<String, Instant>  — O(1) lookup; per-bucket stripe locking means
 *     revoke() and isRevoked() on different tokens never contend.
 *   LinkedBlockingDeque<String>         — bounded audit trail (10 000 entries); drops the
 *     oldest entry when full so the deque never grows past MAX_AUDIT_ENTRIES.
 *
 * isRevoked() uses a single ConcurrentHashMap.compute() call to atomically check expiry
 * and lazily remove stale entries, eliminating the race that existed between the old
 * purgeExpired() + containsKey() two-step.
 */
@Service
public class TokenBlacklistService {

    private static final int MAX_AUDIT_ENTRIES = 10_000;

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();
    private final LinkedBlockingDeque<String>         auditLog  = new LinkedBlockingDeque<>(MAX_AUDIT_ENTRIES);

    public void revoke(String token, Instant expiry) {
        blacklist.put(token, expiry);
        // offerLast() silently drops the offer when the deque is full — oldest entries
        // are already in the blacklist map, so audit coverage is not lost.
        if (!auditLog.offerLast("REVOKED:" + token + "@" + Instant.now())) {
            auditLog.pollFirst();                    // evict oldest, then retry
            auditLog.offerLast("REVOKED:" + token + "@" + Instant.now());
        }
    }

    public boolean isRevoked(String token) {
        // compute() holds the bucket lock for the duration: check + optional remove are atomic.
        Instant expiry = blacklist.compute(token, (k, exp) -> {
            if (exp == null) return null;
            return exp.isBefore(Instant.now()) ? null : exp;   // null removes the entry
        });
        return expiry != null;
    }

    public List<String> getAuditLog() {
        return Collections.unmodifiableList(new ArrayList<>(auditLog));
    }

    /** Removes blacklist entries whose expiry has already passed. Runs every 15 minutes. */
    @Scheduled(fixedDelay = 15 * 60_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
