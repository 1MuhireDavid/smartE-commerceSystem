package org.ecommerce.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregated performance baseline report produced by PerformanceReportService.
 * All nested types use the project's standard DTO style: private final fields,
 * all-args constructor, public getters only.
 */
public class PerformanceReportDto {

    private PerformanceReportDto() {}

    // ── Top-level report ──────────────────────────────────────────────────────

    public static final class PerformanceBaselineReport {

        private final Instant                       capturedAt;
        private final JvmSnapshot                   jvm;
        private final HibernateStats                hibernateStats;
        private final List<ServiceMethodStat>       topSlowMethods;
        private final Map<String, CacheStatsSummary> cacheStats;
        private final List<HttpEndpointStat>        slowestEndpoints;
        private final List<IdentifiedBottleneck>    bottlenecks;

        public PerformanceBaselineReport(Instant capturedAt,
                                         JvmSnapshot jvm,
                                         HibernateStats hibernateStats,
                                         List<ServiceMethodStat> topSlowMethods,
                                         Map<String, CacheStatsSummary> cacheStats,
                                         List<HttpEndpointStat> slowestEndpoints,
                                         List<IdentifiedBottleneck> bottlenecks) {
            this.capturedAt       = capturedAt;
            this.jvm              = jvm;
            this.hibernateStats   = hibernateStats;
            this.topSlowMethods   = topSlowMethods;
            this.cacheStats       = cacheStats;
            this.slowestEndpoints = slowestEndpoints;
            this.bottlenecks      = bottlenecks;
        }

        public Instant                        getCapturedAt()       { return capturedAt; }
        public JvmSnapshot                    getJvm()              { return jvm; }
        public HibernateStats                 getHibernateStats()   { return hibernateStats; }
        public List<ServiceMethodStat>        getTopSlowMethods()   { return topSlowMethods; }
        public Map<String, CacheStatsSummary> getCacheStats()       { return cacheStats; }
        public List<HttpEndpointStat>         getSlowestEndpoints() { return slowestEndpoints; }
        public List<IdentifiedBottleneck>     getBottlenecks()      { return bottlenecks; }
    }

    // ── JVM snapshot ─────────────────────────────────────────────────────────

    public static final class JvmSnapshot {

        private final long   heapUsedMb;
        private final long   heapMaxMb;
        private final double heapUsagePct;
        private final long   nonHeapUsedMb;
        private final double cpuUsagePct;
        private final int    threadCount;
        private final long   gcTotalCount;
        private final long   gcTotalTimeMs;

        public JvmSnapshot(long heapUsedMb, long heapMaxMb, double heapUsagePct,
                           long nonHeapUsedMb, double cpuUsagePct, int threadCount,
                           long gcTotalCount, long gcTotalTimeMs) {
            this.heapUsedMb    = heapUsedMb;
            this.heapMaxMb     = heapMaxMb;
            this.heapUsagePct  = heapUsagePct;
            this.nonHeapUsedMb = nonHeapUsedMb;
            this.cpuUsagePct   = cpuUsagePct;
            this.threadCount   = threadCount;
            this.gcTotalCount  = gcTotalCount;
            this.gcTotalTimeMs = gcTotalTimeMs;
        }

        public long   getHeapUsedMb()    { return heapUsedMb; }
        public long   getHeapMaxMb()     { return heapMaxMb; }
        public double getHeapUsagePct()  { return heapUsagePct; }
        public long   getNonHeapUsedMb() { return nonHeapUsedMb; }
        public double getCpuUsagePct()   { return cpuUsagePct; }
        public int    getThreadCount()   { return threadCount; }
        public long   getGcTotalCount()  { return gcTotalCount; }
        public long   getGcTotalTimeMs() { return gcTotalTimeMs; }
    }

    // ── Hibernate query statistics ────────────────────────────────────────────

    public static final class HibernateStats {

        private final long   queryExecutionCount;
        private final long   queryExecutionMaxTimeMs;
        private final String slowestQueryString;
        private final long   entityLoadCount;

        public HibernateStats(long queryExecutionCount, long queryExecutionMaxTimeMs,
                              String slowestQueryString, long entityLoadCount) {
            this.queryExecutionCount   = queryExecutionCount;
            this.queryExecutionMaxTimeMs = queryExecutionMaxTimeMs;
            this.slowestQueryString    = slowestQueryString;
            this.entityLoadCount       = entityLoadCount;
        }

        public long   getQueryExecutionCount()     { return queryExecutionCount; }
        public long   getQueryExecutionMaxTimeMs() { return queryExecutionMaxTimeMs; }
        public String getSlowestQueryString()      { return slowestQueryString; }
        public long   getEntityLoadCount()         { return entityLoadCount; }
    }

    // ── Per-service-method stat ───────────────────────────────────────────────

    public static final class ServiceMethodStat {

        private final String methodKey;
        private final long   invocations;
        private final long   slowInvocations;
        private final double avgTimeMs;
        private final long   minTimeMs;
        private final long   maxTimeMs;

        public ServiceMethodStat(String methodKey, long invocations, long slowInvocations,
                                 double avgTimeMs, long minTimeMs, long maxTimeMs) {
            this.methodKey       = methodKey;
            this.invocations     = invocations;
            this.slowInvocations = slowInvocations;
            this.avgTimeMs       = avgTimeMs;
            this.minTimeMs       = minTimeMs;
            this.maxTimeMs       = maxTimeMs;
        }

        public String getMethodKey()       { return methodKey; }
        public long   getInvocations()     { return invocations; }
        public long   getSlowInvocations() { return slowInvocations; }
        public double getAvgTimeMs()       { return avgTimeMs; }
        public long   getMinTimeMs()       { return minTimeMs; }
        public long   getMaxTimeMs()       { return maxTimeMs; }
    }

    // ── Caffeine cache stats snapshot ─────────────────────────────────────────

    public static final class CacheStatsSummary {

        private final long   hitCount;
        private final long   missCount;
        private final double hitRate;
        private final long   evictionCount;
        private final long   estimatedSize;

        public CacheStatsSummary(long hitCount, long missCount, double hitRate,
                                 long evictionCount, long estimatedSize) {
            this.hitCount      = hitCount;
            this.missCount     = missCount;
            this.hitRate       = hitRate;
            this.evictionCount = evictionCount;
            this.estimatedSize = estimatedSize;
        }

        public long   getHitCount()      { return hitCount; }
        public long   getMissCount()     { return missCount; }
        public double getHitRate()       { return hitRate; }
        public long   getEvictionCount() { return evictionCount; }
        public long   getEstimatedSize() { return estimatedSize; }
    }

    // ── HTTP endpoint latency stat ────────────────────────────────────────────

    public static final class HttpEndpointStat {

        private final String uri;
        private final String httpMethod;
        private final long   requestCount;
        private final double meanMs;
        private final double maxMs;

        public HttpEndpointStat(String uri, String httpMethod, long requestCount,
                                double meanMs, double maxMs) {
            this.uri          = uri;
            this.httpMethod   = httpMethod;
            this.requestCount = requestCount;
            this.meanMs       = meanMs;
            this.maxMs        = maxMs;
        }

        public String getUri()          { return uri; }
        public String getHttpMethod()   { return httpMethod; }
        public long   getRequestCount() { return requestCount; }
        public double getMeanMs()       { return meanMs; }
        public double getMaxMs()        { return maxMs; }
    }

    // ── Identified bottleneck entry ───────────────────────────────────────────

    public static final class IdentifiedBottleneck {

        private final String severity;
        private final String category;
        private final String location;
        private final String description;
        private final String recommendation;

        public IdentifiedBottleneck(String severity, String category, String location,
                                    String description, String recommendation) {
            this.severity       = severity;
            this.category       = category;
            this.location       = location;
            this.description    = description;
            this.recommendation = recommendation;
        }

        public String getSeverity()       { return severity; }
        public String getCategory()       { return category; }
        public String getLocation()       { return location; }
        public String getDescription()    { return description; }
        public String getRecommendation() { return recommendation; }
    }
}
