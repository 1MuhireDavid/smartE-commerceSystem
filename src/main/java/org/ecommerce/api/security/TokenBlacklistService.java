package org.ecommerce.api.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory token revocation store — Epic 3 (US 3.1).
 *
 * Data structures:
 *   ConcurrentHashMap<String, Instant>  — O(1) lookup; per-bucket stripe locking means
 *     revoke() and isRevoked() on different tokens never contend.
 *   CopyOnWriteArrayList<String>        — audit trail; optimised for frequent reads
 *     (admin audit queries) over rare writes (one append per revocation).
 *
 * isRevoked() uses a single ConcurrentHashMap.compute() call to atomically check expiry
 * and lazily remove stale entries, eliminating the race that existed between the old
 * purgeExpired() + containsKey() two-step.
 */
@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String>        auditLog  = new CopyOnWriteArrayList<>();

    public void revoke(String token, Instant expiry) {
        blacklist.put(token, expiry);
        auditLog.add("REVOKED:" + token + "@" + Instant.now());
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
        return Collections.unmodifiableList(auditLog);
    }

    public int blacklistSize() {
        return blacklist.size();
    }
}
