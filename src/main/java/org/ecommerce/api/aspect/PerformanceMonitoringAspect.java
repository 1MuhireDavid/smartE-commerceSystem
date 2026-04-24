package org.ecommerce.api.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP aspect that measures execution time for every service method and
 * accumulates per-method invocation statistics.
 *
 * <h2>Why @Around?</h2>
 * <p>{@code @Around} is the only advice type that can both intercept the method
 * <em>before</em> it runs <strong>and</strong> observe its return value (or
 * exception) afterwards — making it the natural fit for timing. The aspect
 * calls {@link ProceedingJoinPoint#proceed()} to hand control to the real method
 * and wraps it in a {@code try/finally} so the timer always stops.
 *
 * <h2>What is tracked</h2>
 * <ul>
 *   <li>Total invocation count per method</li>
 *   <li>Slow invocation count (exceeds {@code monitoring.slow-method-threshold-ms})</li>
 *   <li>Cumulative and average execution time per method</li>
 *   <li>Last recorded execution time</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>{@link ConcurrentHashMap#computeIfAbsent} ensures each method key gets
 * exactly one {@link MethodMetrics} instance even under concurrent requests.
 * The metrics object itself uses {@code AtomicLong} for lock-free updates.
 *
 * <h2>Accessing the data</h2>
 * <p>Stats are exposed at {@code GET /api/monitoring/metrics} via
 * {@link org.ecommerce.api.controller.MonitoringController}.
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    /** Threshold (ms) above which a call is flagged as slow. Configurable per profile. */
    @Value("${monitoring.slow-method-threshold-ms:500}")
    private long slowThresholdMs;

    /** Live per-method statistics. Key format: {@code SimpleClassName.methodName}. */
    private final ConcurrentHashMap<String, MethodMetrics> metricsMap = new ConcurrentHashMap<>();

    @Pointcut("execution(public * org.ecommerce.api.service.impl.*.*(..))")
    public void serviceImplMethods() {}

    /**
     * Wraps every service method to measure its wall-clock execution time.
     *
     * <p>Steps:
     * <ol>
     *   <li>Record the start time in nanoseconds.</li>
     *   <li>Call {@code proceed()} — this runs the actual service method.</li>
     *   <li>In {@code finally}: compute elapsed time, update {@link MethodMetrics},
     *       log at DEBUG (or WARN if slow).</li>
     * </ol>
     *
     * <p>The {@code throws Throwable} declaration is required because
     * {@code proceed()} can propagate any checked or unchecked exception thrown
     * by the target method. The aspect re-throws it unchanged — only timing
     * data is added.
     *
     * @param pjp provides access to the intercepted method and the ability to invoke it
     * @return the value returned by the target service method
     * @throws Throwable any exception thrown by the target method, propagated unchanged
     */
    @Around("serviceImplMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig       = (MethodSignature) pjp.getSignature();
        String className          = pjp.getTarget().getClass().getSimpleName();
        String methodName         = sig.getName();
        String key                = className + "." + methodName;

        long startNs = System.nanoTime();
        try {
            return pjp.proceed();                          // ← invoke the real method
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            boolean slow   = elapsedMs > slowThresholdMs;

            metricsMap.computeIfAbsent(key, MethodMetrics::new)
                      .record(elapsedMs, slow);

            if (slow) {
                log.warn("⚠ SLOW SERVICE METHOD  {}.{}()  took {}ms  (threshold: {}ms)",
                         className, methodName, elapsedMs, slowThresholdMs);
            } else {
                log.debug("  PERF  {}.{}()  {}ms", className, methodName, elapsedMs);
            }
        }
    }

    /**
     * Returns an unmodifiable view of the live metrics map.
     * Called by {@link org.ecommerce.api.controller.MonitoringController}.
     */
    public Map<String, MethodMetrics> getMetrics() {
        return Collections.unmodifiableMap(metricsMap);
    }
}
