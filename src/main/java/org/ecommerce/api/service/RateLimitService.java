package org.ecommerce.api.service;

import java.util.List;

public interface RateLimitService {

    /**
     * Returns true if the key (user ID, IP, etc.) is within its rate-limit window.
     * Internally increments the request counter for the current window.
     */
    boolean isAllowed(String key);

    /** Remaining requests the key may make before the current window closes. */
    int getRemainingRequests(String key);

    /** Recent rate-limit violation entries, for admin reporting. */
    List<String> getRecentViolations();
}
