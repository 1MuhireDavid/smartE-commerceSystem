package org.ecommerce.api.service.impl;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManagerFactory;
import org.ecommerce.api.aspect.PerformanceMonitoringAspect;
import org.ecommerce.api.config.AsyncConfig;
import org.ecommerce.api.dto.PerformanceReportDto;
import org.ecommerce.api.dto.PerformanceReportDto.*;
import org.ecommerce.api.service.PerformanceReportService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PerformanceReportServiceImpl implements PerformanceReportService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceReportServiceImpl.class);
    private static final String HTTP_REQUESTS_METRIC = "http.server.requests";

    // Bottleneck catalogue never changes — build once at class load, not per call
    private static final List<IdentifiedBottleneck> BOTTLENECKS = List.of(
        new IdentifiedBottleneck(
            Severity.CRITICAL, BottleneckCategory.N1_QUERY,
            "OrderServiceImpl.findItems()",
            "OrderItemRepository.findByOrder_OrderId() returns items without JOIN FETCH on "
            + "product. Accessing item.getProduct() on each element triggers N additional "
            + "SELECT queries — one per order item.",
            "Add a @EntityGraph or JPQL JOIN FETCH query in OrderItemRepository for the "
            + "product association when loading items by order."
        ),
        new IdentifiedBottleneck(
            Severity.CRITICAL, BottleneckCategory.N1_QUERY,
            "ReviewServiceImpl.findAll()",
            "ReviewRepository.search() returns a Page<ReviewEntity> with three lazily-loaded "
            + "associations: product, user, and order. Iterating the page fires up to 3N "
            + "additional SELECT queries on a single list request.",
            "Add JOIN FETCH for product, user, and order in ReviewRepository.search(), or use "
            + "@EntityGraph(attributePaths = {\"product\", \"user\", \"order\"})."
        ),
        new IdentifiedBottleneck(
            Severity.CRITICAL, BottleneckCategory.N1_QUERY,
            "CartServiceImpl.getItems()",
            "CartItemRepository.findByCart_CartId() returns cart items with LAZY product "
            + "association. Any caller that accesses item.getProduct() — e.g. to return "
            + "price or name — triggers N additional SELECT queries.",
            "Add a JPQL query with JOIN FETCH cart_item.product in CartItemRepository "
            + "for the findByCartId use-case."
        ),
        new IdentifiedBottleneck(
            Severity.CRITICAL, BottleneckCategory.BLOCKING_LOOP,
            "OrderServiceImpl.create()",
            "Order creation iterates over N line items, calling productRepository.findById() "
            + "once per item (N SELECT queries) then orderItemRepository.save() once per item "
            + "(N INSERT statements). For a 10-item order this is 20 sequential DB round-trips.",
            "Batch-load all products with productRepository.findAllById(ids) before the loop "
            + "(1 IN query). Collect order items into a list and save with "
            + "orderItemRepository.saveAll() to leverage JDBC batch inserts."
        ),
        new IdentifiedBottleneck(
            Severity.CRITICAL, BottleneckCategory.MISSING_INDEX,
            "schema.sql — categories table",
            "The categories table has no indexes other than the PK. Filtering or sorting by "
            + "name (LIKE, lower(name)) or is_active results in a full sequential scan on "
            + "every request.",
            "Add: CREATE INDEX idx_categories_name ON categories (lower(name)); "
            + "CREATE INDEX idx_categories_active ON categories (is_active);"
        ),
        new IdentifiedBottleneck(
            Severity.HIGH, BottleneckCategory.CACHE_EVICTION,
            "ProductServiceImpl.create()",
            "@CacheEvict(value=\"products\", allEntries=true) flushes the entire products "
            + "cache whenever a single product is added. All subsequent reads miss the cache "
            + "until entries are individually re-warmed.",
            "Change to @CacheEvict(key=\"#result.productId\") or use a cache-aside pattern "
            + "that only evicts the new entry. The list-view cache (if added) can use a "
            + "separate cache name invalidated by key."
        ),
        new IdentifiedBottleneck(
            Severity.HIGH, BottleneckCategory.UNCACHED_SEARCH,
            "ProductServiceImpl.findAll()",
            "Every call to the product search endpoint executes a native-SQL full-text search "
            + "query (searchFts) against PostgreSQL. Popular keyword searches hit the database "
            + "on every request — no result caching is in place.",
            "Cache the paginated search results: @Cacheable(value=\"productSearch\", "
            + "key=\"{#keyword,#categoryId,#status,#sellerId,#pageable}\") with a short TTL "
            + "(e.g. 2 minutes) in the Caffeine spec."
        ),
        new IdentifiedBottleneck(
            Severity.HIGH, BottleneckCategory.N1_QUERY,
            "PaymentServiceImpl.findAll()",
            "PaymentRepository.search() returns a Page<PaymentEntity> with a lazily-loaded "
            + "order association. Callers that access payment.getOrder() fire N additional "
            + "SELECT queries.",
            "Add JOIN FETCH payment.order in PaymentRepository.search() JPQL query, or "
            + "annotate with @EntityGraph(attributePaths = {\"order\"})."
        ),
        new IdentifiedBottleneck(
            Severity.HIGH, BottleneckCategory.MISSING_INDEX,
            "schema.sql — cart_items table",
            "cart_items has no secondary indexes. Queries that filter by cart_id or "
            + "product_id (e.g. finding items in a cart) perform full-table scans as the "
            + "table grows.",
            "Add: CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id); "
            + "CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);"
        ),
        new IdentifiedBottleneck(
            Severity.MEDIUM, BottleneckCategory.MISSING_INDEX,
            "schema.sql — reviews(product_id, is_approved)",
            "Filtering approved reviews for a product (the public storefront use-case) "
            + "requires scanning all reviews for a product_id without a covering index on "
            + "the (product_id, is_approved) combination.",
            "Add: CREATE INDEX idx_reviews_product_approved ON reviews (product_id, is_approved);"
        ),
        new IdentifiedBottleneck(
            Severity.MEDIUM, BottleneckCategory.MEMORY_LEAK,
            "TokenBlacklistService",
            "Revoked JWT tokens are stored in a ConcurrentHashMap with no expiry or cleanup. "
            + "Over time the map grows unboundedly, increasing heap pressure and slowing "
            + "every token-blacklist lookup.",
            "Schedule a nightly purge with @Scheduled(cron=\"0 0 2 * * *\") that removes "
            + "entries whose associated token expiry timestamp has passed."
        ),
        new IdentifiedBottleneck(
            Severity.MEDIUM, BottleneckCategory.MULTI_QUERY,
            "OrderServiceImpl.getStats()",
            "getStats() issues two separate aggregate queries: orderRepository.getStatsByStatus() "
            + "and orderRepository.sumPaidRevenue(). These two round-trips could be combined "
            + "into a single query.",
            "Combine into one native SQL query that returns both status-group counts and the "
            + "paid revenue sum in a single round-trip, reducing latency by ~50% for the "
            + "stats endpoint."
        ),
        new IdentifiedBottleneck(
            Severity.LOW, BottleneckCategory.CACHE_EVICTION,
            "UserServiceImpl.create()",
            "@CacheEvict(value=\"users\", allEntries=true) on user registration flushes all "
            + "cached user entries. In a write-heavy registration flow this degrades cache "
            + "effectiveness for all concurrent user lookups.",
            "Change to @CacheEvict(key=\"#result.userId\") on create and update, so only "
            + "the affected user entry is invalidated."
        )
    );

    private final PerformanceMonitoringAspect monitoringAspect;
    private final CacheManager               cacheManager;
    private final MeterRegistry              meterRegistry;
    private final EntityManagerFactory       entityManagerFactory;
    private final ThreadPoolTaskExecutor     taskExecutor;

    public PerformanceReportServiceImpl(PerformanceMonitoringAspect monitoringAspect,
                                        CacheManager cacheManager,
                                        MeterRegistry meterRegistry,
                                        EntityManagerFactory entityManagerFactory,
                                        @Qualifier(AsyncConfig.EXECUTOR_BEAN)
                                        ThreadPoolTaskExecutor taskExecutor) {
        this.monitoringAspect     = monitoringAspect;
        this.cacheManager         = cacheManager;
        this.meterRegistry        = meterRegistry;
        this.entityManagerFactory = entityManagerFactory;
        this.taskExecutor         = taskExecutor;
    }

    @Override
    public PerformanceBaselineReport generateReport() {
        // HTTP timers are scanned once; both slowestEndpoints and throughput share the result.
        List<HttpEndpointStat>  httpStats  = captureHttpStats();
        ThroughputSnapshot      throughput = computeThroughput(httpStats);

        return new PerformanceBaselineReport(
                Instant.now(),
                captureJvm(),
                captureHibernateStats(),
                captureTopSlowMethods(),
                captureCacheStats(),
                httpStats,
                throughput,
                getBottlenecks()
        );
    }

    @Override
    public List<IdentifiedBottleneck> getBottlenecks() {
        return BOTTLENECKS;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double round1dp(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private JvmSnapshot captureJvm() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap    = memBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

        long heapUsedMb    = heap.getUsed()    / (1024 * 1024);
        long heapMaxMb     = heap.getMax()     / (1024 * 1024);
        long nonHeapUsedMb = nonHeap.getUsed() / (1024 * 1024);
        double heapUsagePct = heapMaxMb > 0
                ? Math.round((double) heapUsedMb / heapMaxMb * 10000.0) / 100.0
                : 0.0;

        double cpuUsagePct = -1.0;
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            cpuUsagePct = round1dp(sunOsBean.getProcessCpuLoad() * 100.0);
        }

        int  threadCount   = ManagementFactory.getThreadMXBean().getThreadCount();
        long gcTotalCount  = 0;
        long gcTotalTimeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            long time  = gc.getCollectionTime();
            if (count > 0) gcTotalCount  += count;
            if (time  > 0) gcTotalTimeMs += time;
        }

        return new JvmSnapshot(heapUsedMb, heapMaxMb, heapUsagePct,
                nonHeapUsedMb, cpuUsagePct, threadCount, gcTotalCount, gcTotalTimeMs);
    }

    private HibernateStats captureHibernateStats() {
        try {
            Statistics stats = entityManagerFactory
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            return new HibernateStats(
                    stats.getQueryExecutionCount(),
                    stats.getQueryExecutionMaxTime(),
                    stats.getQueryExecutionMaxTimeQueryString(),
                    stats.getEntityLoadCount()
            );
        } catch (Exception e) {
            log.warn("Hibernate statistics unavailable — is generate_statistics=true? {}", e.getMessage());
            return new HibernateStats(0L, 0L, "statistics-unavailable", 0L);
        }
    }

    private List<ServiceMethodStat> captureTopSlowMethods() {
        return monitoringAspect.getMetrics().values().stream()
                .map(m -> new ServiceMethodStat(
                        m.getMethodKey(),
                        m.getInvocations(),
                        m.getSlowInvocations(),
                        round1dp(m.getAvgTimeMs()),
                        m.getMinTimeMs(),
                        m.getMaxTimeMs(),
                        m.getLastTimeMs()
                ))
                .sorted(Comparator.comparingDouble(ServiceMethodStat::getAvgTimeMs).reversed())
                .limit(10)
                .toList();
    }

    @Override
    public Map<String, CacheStatsSummary> captureCacheStats() {
        Map<String, CacheStatsSummary> result = new LinkedHashMap<>();
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache instanceof CaffeineCache caffeineCache) {
                CacheStats stats = caffeineCache.getNativeCache().stats();
                result.put(name, new CacheStatsSummary(
                        stats.hitCount(),
                        stats.missCount(),
                        round1dp(stats.hitRate()),
                        stats.evictionCount(),
                        caffeineCache.getNativeCache().estimatedSize()
                ));
            }
        });
        return result;
    }

    private List<HttpEndpointStat> captureHttpStats() {
        Collection<Timer> timers = meterRegistry.find(HTTP_REQUESTS_METRIC).timers();
        if (timers.isEmpty()) return Collections.emptyList();

        return timers.stream()
                .map(t -> {
                    // percentile() returns NaN if management.metrics.distribution.percentiles
                    // is not configured — guard with 0.0 fallback for clean JSON output.
                    double p95 = t.percentile(0.95, TimeUnit.MILLISECONDS);
                    double p99 = t.percentile(0.99, TimeUnit.MILLISECONDS);
                    return new HttpEndpointStat(
                            t.getId().getTag("uri"),
                            t.getId().getTag("method"),
                            (long) t.count(),
                            round1dp(t.mean(TimeUnit.MILLISECONDS)),
                            Double.isNaN(p95) ? 0.0 : round1dp(p95),
                            Double.isNaN(p99) ? 0.0 : round1dp(p99),
                            round1dp(t.max(TimeUnit.MILLISECONDS))
                    );
                })
                .filter(s -> s.getRequestCount() > 0)
                .sorted(Comparator.comparingDouble(HttpEndpointStat::getMeanMs).reversed())
                .limit(10)
                .toList();
    }

    /** Derives throughput from an already-collected http stats list to avoid a second timer scan. */
    private ThroughputSnapshot computeThroughput(List<HttpEndpointStat> httpStats) {
        long uptimeSec     = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long totalRequests = httpStats.stream().mapToLong(HttpEndpointStat::getRequestCount).sum();
        double avgRps      = uptimeSec > 0 ? round1dp((double) totalRequests / uptimeSec) : 0.0;

        List<PerformanceReportDto.EndpointThroughput> top = httpStats.stream()
                .map(s -> new PerformanceReportDto.EndpointThroughput(
                        s.getUri(),
                        s.getHttpMethod(),
                        s.getRequestCount(),
                        uptimeSec > 0 ? round1dp((double) s.getRequestCount() / uptimeSec) : 0.0
                ))
                .sorted(Comparator.comparingDouble(
                        PerformanceReportDto.EndpointThroughput::getRequestsPerSecond).reversed())
                .toList();

        return new ThroughputSnapshot(uptimeSec, totalRequests, avgRps, top);
    }

    @Override
    public ThroughputSnapshot getThroughputSnapshot() {
        return computeThroughput(captureHttpStats());
    }

    @Override
    public Map<String, PerformanceReportDto.ServiceMethodStat> captureAllMethodMetrics() {
        Map<String, PerformanceReportDto.ServiceMethodStat> result = new LinkedHashMap<>();
        monitoringAspect.getMetrics().forEach((key, m) ->
                result.put(key, new ServiceMethodStat(
                        m.getMethodKey(),
                        m.getInvocations(),
                        m.getSlowInvocations(),
                        round1dp(m.getAvgTimeMs()),
                        m.getMinTimeMs(),
                        m.getMaxTimeMs(),
                        m.getLastTimeMs()
                ))
        );
        return result;
    }

    @Override
    public ThreadPoolStats captureThreadPoolStats() {
        java.util.concurrent.BlockingQueue<?> queue = taskExecutor.getThreadPoolExecutor().getQueue();
        int queueCapacity = queue.size() + queue.remainingCapacity();
        return new ThreadPoolStats(
                taskExecutor.getCorePoolSize(),
                taskExecutor.getMaxPoolSize(),
                taskExecutor.getActiveCount(),
                taskExecutor.getPoolSize(),
                queue.size(),
                queueCapacity,
                taskExecutor.getThreadPoolExecutor().getCompletedTaskCount()
        );
    }
}
