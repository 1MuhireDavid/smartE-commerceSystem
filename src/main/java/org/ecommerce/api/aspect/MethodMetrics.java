package org.ecommerce.api.aspect;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * Thread-safe per-method invocation statistics collected by
 * {@link PerformanceMonitoringAspect}.
 * Exposed at {@code GET /api/monitoring/metrics}.
 */
public class MethodMetrics {

    private final String          methodKey;
    private final AtomicLong      invocations     = new AtomicLong();
    private final AtomicLong      slowInvocations = new AtomicLong();
    private final AtomicLong      totalTimeMs     = new AtomicLong();
    private final AtomicLong      lastTimeMs      = new AtomicLong();
    // LongAccumulator avoids CAS spin-loops under high concurrency
    private final LongAccumulator minTimeMs       = new LongAccumulator(Math::min, Long.MAX_VALUE);
    private final LongAccumulator maxTimeMs       = new LongAccumulator(Math::max, 0L);

    public MethodMetrics(String methodKey) {
        this.methodKey = methodKey;
    }

    public void record(long elapsedMs, boolean slow) {
        invocations.incrementAndGet();
        totalTimeMs.addAndGet(elapsedMs);
        lastTimeMs.set(elapsedMs);
        if (slow) slowInvocations.incrementAndGet();
        minTimeMs.accumulate(elapsedMs);
        maxTimeMs.accumulate(elapsedMs);
    }

    public String getMethodKey()       { return methodKey; }
    public long   getInvocations()     { return invocations.get(); }
    public long   getSlowInvocations() { return slowInvocations.get(); }
    public long   getLastTimeMs()      { return lastTimeMs.get(); }
    public long   getMaxTimeMs()       { return maxTimeMs.get(); }

    /** Returns 0 if no invocations have been recorded yet. */
    public long getMinTimeMs() {
        long v = minTimeMs.get();
        return v == Long.MAX_VALUE ? 0L : v;
    }

    public double getAvgTimeMs() {
        long inv = invocations.get();
        return inv == 0 ? 0.0 : (double) totalTimeMs.get() / inv;
    }
}
