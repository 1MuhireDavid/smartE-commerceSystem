package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.api.aspect.MethodMetrics;
import org.example.api.aspect.PerformanceMonitoringAspect;
import org.example.api.dto.ApiResponse;
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

    public MonitoringController(PerformanceMonitoringAspect monitoringAspect) {
        this.monitoringAspect = monitoringAspect;
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
    public ResponseEntity<org.example.api.dto.ApiResponse<Map<String, MetricsSummary>>> metrics() {
        Map<String, MethodMetrics>  raw     = monitoringAspect.getMetrics();
        Map<String, MetricsSummary> summary = new LinkedHashMap<>();

        raw.forEach((key, m) -> summary.put(key, new MetricsSummary(m)));

        return ResponseEntity.ok(
                org.example.api.dto.ApiResponse.success("Metrics retrieved", summary));
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
}
