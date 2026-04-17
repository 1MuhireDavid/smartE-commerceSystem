package org.ecommerce.api.aspect;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe per-method invocation statistics collected by
 * {@link PerformanceMonitoringAspect}.
 *
 * <p>All fields use {@link AtomicLong} so concurrent service calls
 * update the counters without synchronisation overhead.
 *
 * <p>Exposed via {@link org.ecommerce.api.controller.MonitoringController}
 * at {@code GET /api/monitoring/metrics}.
 */
public class MethodMetrics {

    private final String     methodKey;
    private final AtomicLong invocations     = new AtomicLong();
    private final AtomicLong slowInvocations = new AtomicLong();
    private final AtomicLong totalTimeMs     = new AtomicLong();
    private final AtomicLong lastTimeMs      = new AtomicLong();

    public MethodMetrics(String methodKey) {
        this.methodKey = methodKey;
    }

    /**
     * Records one completed invocation.
     *
     * @param elapsedMs execution time in milliseconds
     * @param slow      whether this call exceeded the configured threshold
     */
    public void record(long elapsedMs, boolean slow) {
        invocations.incrementAndGet();
        totalTimeMs.addAndGet(elapsedMs);
        lastTimeMs.set(elapsedMs);
        if (slow) {
            slowInvocations.incrementAndGet();
        }
    }

    public String getMethodKey()       { return methodKey; }
    public long   getInvocations()     { return invocations.get(); }
    public long   getSlowInvocations() { return slowInvocations.get(); }
    public long   getLastTimeMs()      { return lastTimeMs.get(); }

    /** Average execution time across all recorded invocations, in milliseconds. */
    public double getAvgTimeMs() {
        long inv = invocations.get();
        return inv == 0 ? 0.0 : (double) totalTimeMs.get() / inv;
    }
}
