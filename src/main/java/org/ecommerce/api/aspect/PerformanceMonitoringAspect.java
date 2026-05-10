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
 * AOP aspect that measures wall-clock execution time for every service method and
 * accumulates per-method invocation statistics (count, slow count, avg/min/max/last ms).
 * Stats are exposed at {@code GET /api/monitoring/metrics} via MonitoringController.
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

    @Around("serviceImplMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig  = (MethodSignature) pjp.getSignature();
        String className     = pjp.getTarget().getClass().getSimpleName();
        String methodName    = sig.getName();
        String key           = className + "." + methodName;

        long startNs = System.nanoTime();
        try {
            return pjp.proceed();
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

    public Map<String, MethodMetrics> getMetrics() {
        return Collections.unmodifiableMap(metricsMap);
    }
}
