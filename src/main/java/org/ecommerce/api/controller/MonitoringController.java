package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ecommerce.api.aspect.MethodMetrics;
import org.ecommerce.api.aspect.PerformanceMonitoringAspect;
import org.ecommerce.api.config.AsyncConfig;
import org.ecommerce.api.dto.PerformanceReportDto;
import org.ecommerce.api.service.ActivityLogService;
import org.ecommerce.api.service.PerformanceReportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the live per-method performance metrics collected by
 * {@link PerformanceMonitoringAspect}.
 *
 * <p>This controller intentionally has no business logic — it is a pure
 * read-through to the aspect's in-memory metrics map.
 */
@Tag(name = "Monitoring", description = "Live AOP-collected service performance metrics")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final PerformanceMonitoringAspect monitoringAspect;
    private final ActivityLogService          activityLogService;
    private final PerformanceReportService    performanceReportService;
    private final ThreadPoolTaskExecutor      taskExecutor;

    public MonitoringController(PerformanceMonitoringAspect monitoringAspect,
                                ActivityLogService activityLogService,
                                PerformanceReportService performanceReportService,
                                @Qualifier(AsyncConfig.EXECUTOR_BEAN) ThreadPoolTaskExecutor taskExecutor) {
        this.monitoringAspect        = monitoringAspect;
        this.activityLogService      = activityLogService;
        this.performanceReportService = performanceReportService;
        this.taskExecutor            = taskExecutor;
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
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<Map<String, PerformanceReportDto.CacheStatsSummary>>> cacheStats() {
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success(
                        "Cache statistics retrieved",
                        performanceReportService.captureCacheStats()));
    }

    @Operation(
        summary     = "Get full performance baseline report (US 1.1 / 1.2)",
        description = "Aggregates JVM heap/CPU/GC, Hibernate query statistics, top-10 slowest "
                    + "service methods (with min/avg/max), Caffeine cache hit rates, top-10 "
                    + "slowest HTTP endpoints, and the full bottleneck catalogue into a single "
                    + "point-in-time snapshot. Use this to capture the baseline before applying "
                    + "optimisations in later Epics."
    )
    @ApiResponse(responseCode = "200", description = "Performance report generated successfully")
    @GetMapping("/performance-report")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<PerformanceReportDto.PerformanceBaselineReport>> performanceReport() {
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success(
                        "Performance report generated",
                        performanceReportService.generateReport()));
    }

    @Operation(
        summary     = "Get identified performance bottlenecks (US 1.2)",
        description = "Returns the static catalogue of 13 bottlenecks identified via code analysis, "
                    + "ordered by severity (CRITICAL → HIGH → MEDIUM → LOW). Each entry includes "
                    + "category, location, description, and a concrete recommendation."
    )
    @ApiResponse(responseCode = "200", description = "Bottlenecks retrieved successfully")
    @GetMapping("/bottlenecks")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<List<PerformanceReportDto.IdentifiedBottleneck>>> bottlenecks() {
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success(
                        "Bottlenecks identified",
                        performanceReportService.getBottlenecks()));
    }

    @Operation(
        summary     = "Thread pool live statistics (US 3.2)",
        description = "Returns a real-time snapshot of the ecommerceTaskExecutor: core/max pool "
                    + "sizes, active thread count, current pool size, queue depth, queue capacity, "
                    + "and cumulative completed task count. Use this during load tests to observe "
                    + "whether the pool is saturated and tune async.executor.* in application.yml."
    )
    @ApiResponse(responseCode = "200", description = "Thread pool statistics retrieved successfully")
    @GetMapping("/thread-pool-stats")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<PerformanceReportDto.ThreadPoolStats>> threadPoolStats() {
        java.util.concurrent.BlockingQueue<?> queue = taskExecutor.getThreadPoolExecutor().getQueue();
        int queueCapacity = queue.size() + queue.remainingCapacity();
        PerformanceReportDto.ThreadPoolStats stats = new PerformanceReportDto.ThreadPoolStats(
                taskExecutor.getCorePoolSize(),
                taskExecutor.getMaxPoolSize(),
                taskExecutor.getActiveCount(),
                taskExecutor.getPoolSize(),
                queue.size(),
                queueCapacity,
                taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Thread pool statistics retrieved", stats));
    }

    @Operation(
        summary     = "Request throughput snapshot (US 5.1)",
        description = "Returns application uptime, total HTTP requests served, average requests/second "
                    + "since start-up, and a per-endpoint breakdown sorted by RPS. Derived from "
                    + "Micrometer's http.server.requests timers — values accumulate from the last restart."
    )
    @ApiResponse(responseCode = "200", description = "Throughput snapshot retrieved successfully")
    @GetMapping("/throughput")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<PerformanceReportDto.ThroughputSnapshot>> throughput() {
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success(
                        "Throughput snapshot retrieved",
                        performanceReportService.getThroughputSnapshot()));
    }

    @Operation(
        summary     = "Get security event report (US 5.2)",
        description = "Returns per-event-type counts from the activity_logs table: "
                    + "login_success, login_failure, register_success, oauth2_login_success, "
                    + "oauth2_login_failure, logout. Use this to detect unusual access patterns "
                    + "or brute-force login attempts."
    )
    @ApiResponse(responseCode = "200", description = "Security report retrieved successfully")
    @GetMapping("/security-report")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<Map<String, Long>>> securityReport() {
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success(
                        "Security report retrieved", activityLogService.countByEventType()));
    }

    private static double round1dp(double v) {
        return Math.round(v * 10.0) / 10.0;
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
            this.avgTimeMs       = round1dp(m.getAvgTimeMs());
            this.lastTimeMs      = m.getLastTimeMs();
        }

        public String getMethodKey()       { return methodKey; }
        public long   getInvocations()     { return invocations; }
        public long   getSlowInvocations() { return slowInvocations; }
        public double getAvgTimeMs()       { return avgTimeMs; }
        public long   getLastTimeMs()      { return lastTimeMs; }
    }

}
