package org.ecommerce.api.monitoring;

import org.ecommerce.api.dto.PerformanceReportDto;
import org.ecommerce.api.service.PerformanceReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Periodically writes a compact metrics snapshot to the application log — Epic 5 (US 5.1).
 *
 * Active only in the {@code dev} profile.  The fixed-rate is configurable via
 * {@code metrics.log-interval-ms} (default 60 s) so CI/load-test runs can lower it
 * without a code change.
 *
 * Log format (one line per interval):
 * <pre>
 * [METRICS-SNAPSHOT] heap=245Mb/768Mb(31.9%) cpu=8.2% threads=42 gc=12ms |
 *   topMethod=ProductServiceImpl.findAll avg=3.2ms×847 slowCalls=0 |
 *   cache: products hit=92.1% size=312 | throughput=4.7 req/s total=16920
 * </pre>
 */
@Component
@Profile("dev")
public class MetricsScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsScheduler.class);

    private final PerformanceReportService performanceReportService;

    public MetricsScheduler(PerformanceReportService performanceReportService) {
        this.performanceReportService = performanceReportService;
    }

    @Scheduled(fixedRateString = "${metrics.log-interval-ms:60000}")
    public void logMetricsSnapshot() {
        try {
            // generateReport() gathers HTTP timers once; throughput is derived from the same data.
            PerformanceReportDto.PerformanceBaselineReport report =
                    performanceReportService.generateReport();

            PerformanceReportDto.JvmSnapshot       jvm       = report.getJvm();
            PerformanceReportDto.ThroughputSnapshot throughput = report.getThroughput();

            String topMethod = "none";
            double topAvg    = 0.0;
            long   topSlow   = 0L;
            if (!report.getTopSlowMethods().isEmpty()) {
                PerformanceReportDto.ServiceMethodStat m = report.getTopSlowMethods().get(0);
                topMethod = m.getMethodKey();
                topAvg    = m.getAvgTimeMs();
                topSlow   = m.getSlowInvocations();
            }

            String cacheInfo = buildCacheInfo(report.getCacheStats());

            log.info("[METRICS-SNAPSHOT] heap={}Mb/{}Mb({}%) cpu={}% threads={} gc={}ms | "
                   + "topMethod={} avg={}ms×{} slowCalls={} | "
                   + "{} | "
                   + "throughput={} req/s total={}",
                    jvm.getHeapUsedMb(), jvm.getHeapMaxMb(), jvm.getHeapUsagePct(),
                    jvm.getCpuUsagePct(), jvm.getThreadCount(), jvm.getGcTotalTimeMs(),
                    topMethod, topAvg,
                    report.getTopSlowMethods().isEmpty() ? 0 : report.getTopSlowMethods().get(0).getInvocations(),
                    topSlow,
                    cacheInfo,
                    throughput.getAvgRequestsPerSecond(), throughput.getTotalRequests());
        } catch (Exception e) {
            log.warn("[METRICS-SNAPSHOT] failed to capture snapshot: {}", e.getMessage());
        }
    }

    private static String buildCacheInfo(Map<String, PerformanceReportDto.CacheStatsSummary> cacheStats) {
        if (cacheStats.isEmpty()) return "cache: empty";
        StringBuilder sb = new StringBuilder("cache:");
        cacheStats.forEach((name, stats) ->
                sb.append(" ").append(name)
                  .append(" hit=").append(Math.round(stats.getHitRate() * 1000.0) / 10.0).append("%")
                  .append(" size=").append(stats.getEstimatedSize())
        );
        return sb.toString();
    }
}
