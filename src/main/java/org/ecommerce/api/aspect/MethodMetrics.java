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
    private final AtomicLong minTimeMs       = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxTimeMs       = new AtomicLong(0L);

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
        if (slow) slowInvocations.incrementAndGet();
        updateMin(elapsedMs);
        updateMax(elapsedMs);
    }

    private void updateMin(long elapsedMs) {
        long current;
        do {
            current = minTimeMs.get();
        } while (elapsedMs < current && !minTimeMs.compareAndSet(current, elapsedMs));
    }

    private void updateMax(long elapsedMs) {
        long current;
        do {
            current = maxTimeMs.get();
        } while (elapsedMs > current && !maxTimeMs.compareAndSet(current, elapsedMs));
    }

    public String getMethodKey()       { return methodKey; }
    public long   getInvocations()     { return invocations.get(); }
    public long   getSlowInvocations() { return slowInvocations.get(); }
    public long   getLastTimeMs()      { return lastTimeMs.get(); }
    public long   getMaxTimeMs()       { return maxTimeMs.get(); }

    /** Returns the minimum recorded execution time, or 0 if no invocations yet. */
    public long getMinTimeMs() {
        long v = minTimeMs.get();
        return v == Long.MAX_VALUE ? 0L : v;
    }

    /** Average execution time across all recorded invocations, in milliseconds. */
    public double getAvgTimeMs() {
        long inv = invocations.get();
        return inv == 0 ? 0.0 : (double) totalTimeMs.get() / inv;
    }
}
