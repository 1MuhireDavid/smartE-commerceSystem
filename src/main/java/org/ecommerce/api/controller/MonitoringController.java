package org.ecommerce.api.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ecommerce.api.aspect.MethodMetrics;
import org.ecommerce.api.aspect.PerformanceMonitoringAspect;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes the live per-method performance metrics collected by
 * {@link PerformanceMonitoringAspect}.
 *
 * <p>This controller intentionally has no business logic — it is a pure
 * read-through to the aspect's in-memory metrics map.
 */
@Tag(name = "Monitoring", description = "Live AOP-collected service performance metrics")
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final PerformanceMonitoringAspect monitoringAspect;
    private final CacheManager               cacheManager;

    public MonitoringController(PerformanceMonitoringAspect monitoringAspect,
                                CacheManager cacheManager) {
        this.monitoringAspect = monitoringAspect;
        this.cacheManager     = cacheManager;
    }

    /**
     * Returns invocation counts, average time, slow-call counts, and last
     * recorded execution time for every service method that has been called
     * at least once since the application started.
     *
     * <p>Stats reset on application restart (in-memory only).
     *
     * <p>Example response:
     * <pre>
     * {
     *   "status": "success",
     *   "message": "Metrics retrieved",
     *   "data": {
     *     "ProductServiceImpl.findAll": {
     *       "methodKey":        "ProductServiceImpl.findAll",
     *       "invocations":      42,
     *       "slowInvocations":  2,
     *       "avgTimeMs":        38.7,
     *       "lastTimeMs":       45
     *     }
     *   }
     * }
     * </pre>
     */
    @Operation(
        summary     = "Get service performance metrics",
        description = "Returns live invocation counts and execution-time statistics collected "
                    + "by the PerformanceMonitoringAspect (@Around) for all service methods "
                    + "called since startup. Methods not yet called are absent from the map."
    )
    @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    @GetMapping("/metrics")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<Map<String, MetricsSummary>>> metrics() {
        Map<String, MethodMetrics>  raw     = monitoringAspect.getMetrics();
        Map<String, MetricsSummary> summary = new LinkedHashMap<>();

        raw.forEach((key, m) -> summary.put(key, new MetricsSummary(m)));

        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Metrics retrieved", summary));
    }

    @Operation(
        summary     = "Get cache statistics",
        description = "Returns hit/miss counts, hit rate, eviction count, and estimated size "
                    + "for each Caffeine cache (products, categories, users). "
                    + "Requires recordStats in the Caffeine spec (already set in application.yml)."
    )
    @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully")
    @GetMapping("/cache-stats")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<Map<String, CacheStatsSummary>>> cacheStats() {
        Map<String, CacheStatsSummary> result = new LinkedHashMap<>();

        cacheManager.getCacheNames().forEach(name -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache instanceof CaffeineCache caffeineCache) {
                CacheStats stats = caffeineCache.getNativeCache().stats();
                result.put(name, new CacheStatsSummary(
                        stats.hitCount(),
                        stats.missCount(),
                        stats.hitRate(),
                        stats.evictionCount(),
                        caffeineCache.getNativeCache().estimatedSize()));
            }
        });

        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Cache statistics retrieved", result));
    }

    // ── Serialisable projection of MethodMetrics ──────────────────────────────

    /**
     * Snapshot DTO — converts {@link MethodMetrics} atomic state into plain
     * Java types that Jackson can serialise without reflection on AtomicLong.
     */
    public static final class MetricsSummary {

        private final String methodKey;
        private final long   invocations;
        private final long   slowInvocations;
        private final double avgTimeMs;
        private final long   lastTimeMs;

        MetricsSummary(MethodMetrics m) {
            this.methodKey       = m.getMethodKey();
            this.invocations     = m.getInvocations();
            this.slowInvocations = m.getSlowInvocations();
            this.avgTimeMs       = Math.round(m.getAvgTimeMs() * 10.0) / 10.0;
            this.lastTimeMs      = m.getLastTimeMs();
        }

        public String getMethodKey()       { return methodKey; }
        public long   getInvocations()     { return invocations; }
        public long   getSlowInvocations() { return slowInvocations; }
        public double getAvgTimeMs()       { return avgTimeMs; }
        public long   getLastTimeMs()      { return lastTimeMs; }
    }

    // ── Cache stats projection ─────────────────────────────────────────────────

    public static final class CacheStatsSummary {

        private final long   hitCount;
        private final long   missCount;
        private final double hitRate;
        private final long   evictionCount;
        private final long   estimatedSize;

        CacheStatsSummary(long hitCount, long missCount, double hitRate,
                          long evictionCount, long estimatedSize) {
            this.hitCount      = hitCount;
            this.missCount     = missCount;
            this.hitRate       = Math.round(hitRate * 1000.0) / 1000.0;
            this.evictionCount = evictionCount;
            this.estimatedSize = estimatedSize;
        }

        public long   getHitCount()      { return hitCount; }
        public long   getMissCount()     { return missCount; }
        public double getHitRate()       { return hitRate; }
        public long   getEvictionCount() { return evictionCount; }
        public long   getEstimatedSize() { return estimatedSize; }
    }
}
