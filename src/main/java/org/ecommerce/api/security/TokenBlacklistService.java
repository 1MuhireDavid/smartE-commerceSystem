package org.ecommerce.api.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token revocation store backed by a ConcurrentHashMap<String, Instant>.
 *
 * US 5.1 — DSA rationale:
 *   HashMap lookup is O(1) average, making revocation checks negligible overhead per
 *   request compared to the JWT signature verification that already happens. Expired
 *   entries are purged lazily on every isRevoked() call, keeping memory bounded without
 *   a background scheduler.
 *
 * Trade-off: state is lost on restart. For production, back this with Redis or a
 * revoked_tokens table and add a @Scheduled nightly purge.
 */
@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiry) {
        blacklist.put(token, expiry);
    }

    public boolean isRevoked(String token) {
        purgeExpired();
        return blacklist.containsKey(token);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
