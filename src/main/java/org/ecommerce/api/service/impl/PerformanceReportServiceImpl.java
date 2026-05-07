package org.ecommerce.api.service.impl;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManagerFactory;
import org.ecommerce.api.aspect.MethodMetrics;
import org.ecommerce.api.aspect.PerformanceMonitoringAspect;
import org.ecommerce.api.dto.PerformanceReportDto;
import org.ecommerce.api.dto.PerformanceReportDto.*;
import org.ecommerce.api.service.PerformanceReportService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PerformanceReportServiceImpl implements PerformanceReportService {

    private final PerformanceMonitoringAspect monitoringAspect;
    private final CacheManager               cacheManager;
    private final MeterRegistry              meterRegistry;
    private final EntityManagerFactory       entityManagerFactory;

    public PerformanceReportServiceImpl(PerformanceMonitoringAspect monitoringAspect,
                                        CacheManager cacheManager,
                                        MeterRegistry meterRegistry,
                                        EntityManagerFactory entityManagerFactory) {
        this.monitoringAspect      = monitoringAspect;
        this.cacheManager          = cacheManager;
        this.meterRegistry         = meterRegistry;
        this.entityManagerFactory  = entityManagerFactory;
    }

    @Override
    public PerformanceBaselineReport generateReport() {
        return new PerformanceBaselineReport(
                Instant.now(),
                captureJvm(),
                captureHibernateStats(),
                captureTopSlowMethods(),
                captureCacheStats(),
                captureHttpStats(),
                getBottlenecks()
        );
    }

    @Override
    public List<IdentifiedBottleneck> getBottlenecks() {
        return List.of(
            new IdentifiedBottleneck(
                "CRITICAL", "N+1_QUERY",
                "OrderServiceImpl.findItems()",
                "OrderItemRepository.findByOrder_OrderId() returns items without JOIN FETCH on "
                + "product. Accessing item.getProduct() on each element triggers N additional "
                + "SELECT queries — one per order item.",
                "Add a @EntityGraph or JPQL JOIN FETCH query in OrderItemRepository for the "
                + "product association when loading items by order."
            ),
            new IdentifiedBottleneck(
                "CRITICAL", "N+1_QUERY",
                "ReviewServiceImpl.findAll()",
                "ReviewRepository.search() returns a Page<ReviewEntity> with three lazily-loaded "
                + "associations: product, user, and order. Iterating the page fires up to 3N "
                + "additional SELECT queries on a single list request.",
                "Add JOIN FETCH for product, user, and order in ReviewRepository.search(), or use "
                + "@EntityGraph(attributePaths = {\"product\", \"user\", \"order\"})."
            ),
            new IdentifiedBottleneck(
                "CRITICAL", "N+1_QUERY",
                "CartServiceImpl.getItems()",
                "CartItemRepository.findByCart_CartId() returns cart items with LAZY product "
                + "association. Any caller that accesses item.getProduct() — e.g. to return "
                + "price or name — triggers N additional SELECT queries.",
                "Add a JPQL query with JOIN FETCH cart_item.product in CartItemRepository "
                + "for the findByCartId use-case."
            ),
            new IdentifiedBottleneck(
                "CRITICAL", "BLOCKING_LOOP",
                "OrderServiceImpl.create()",
                "Order creation iterates over N line items, calling productRepository.findById() "
                + "once per item (N SELECT queries) then orderItemRepository.save() once per item "
                + "(N INSERT statements). For a 10-item order this is 20 sequential DB round-trips.",
                "Batch-load all products with productRepository.findAllById(ids) before the loop "
                + "(1 IN query). Collect order items into a list and save with "
                + "orderItemRepository.saveAll() to leverage JDBC batch inserts."
            ),
            new IdentifiedBottleneck(
                "CRITICAL", "MISSING_INDEX",
                "schema.sql — categories table",
                "The categories table has no indexes other than the PK. Filtering or sorting by "
                + "name (LIKE, lower(name)) or is_active results in a full sequential scan on "
                + "every request.",
                "Add: CREATE INDEX idx_categories_name ON categories (lower(name)); "
                + "CREATE INDEX idx_categories_active ON categories (is_active);"
            ),
            new IdentifiedBottleneck(
                "HIGH", "CACHE_EVICTION",
                "ProductServiceImpl.create()",
                "@CacheEvict(value=\"products\", allEntries=true) flushes the entire products "
                + "cache whenever a single product is added. All subsequent reads miss the cache "
                + "until entries are individually re-warmed.",
                "Change to @CacheEvict(key=\"#result.productId\") or use a cache-aside pattern "
                + "that only evicts the new entry. The list-view cache (if added) can use a "
                + "separate cache name invalidated by key."
            ),
            new IdentifiedBottleneck(
                "HIGH", "UNCACHED_SEARCH",
                "ProductServiceImpl.findAll()",
                "Every call to the product search endpoint executes a native-SQL full-text search "
                + "query (searchFts) against PostgreSQL. Popular keyword searches hit the database "
                + "on every request — no result caching is in place.",
                "Cache the paginated search results: @Cacheable(value=\"productSearch\", "
                + "key=\"{#keyword,#categoryId,#status,#sellerId,#pageable}\") with a short TTL "
                + "(e.g. 2 minutes) in the Caffeine spec."
            ),
            new IdentifiedBottleneck(
                "HIGH", "N+1_QUERY",
                "PaymentServiceImpl.findAll()",
                "PaymentRepository.search() returns a Page<PaymentEntity> with a lazily-loaded "
                + "order association. Callers that access payment.getOrder() fire N additional "
                + "SELECT queries.",
                "Add JOIN FETCH payment.order in PaymentRepository.search() JPQL query, or "
                + "annotate with @EntityGraph(attributePaths = {\"order\"})."
            ),
            new IdentifiedBottleneck(
                "HIGH", "MISSING_INDEX",
                "schema.sql — cart_items table",
                "cart_items has no secondary indexes. Queries that filter by cart_id or "
                + "product_id (e.g. finding items in a cart) perform full-table scans as the "
                + "table grows.",
                "Add: CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id); "
                + "CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);"
            ),
            new IdentifiedBottleneck(
                "MEDIUM", "MISSING_INDEX",
                "schema.sql — reviews(product_id, is_approved)",
                "Filtering approved reviews for a product (the public storefront use-case) "
                + "requires scanning all reviews for a product_id without a covering index on "
                + "the (product_id, is_approved) combination.",
                "Add: CREATE INDEX idx_reviews_product_approved ON reviews (product_id, is_approved);"
            ),
            new IdentifiedBottleneck(
                "MEDIUM", "MEMORY_LEAK",
                "TokenBlacklistService",
                "Revoked JWT tokens are stored in a ConcurrentHashMap with no expiry or cleanup. "
                + "Over time the map grows unboundedly, increasing heap pressure and slowing "
                + "every token-blacklist lookup.",
                "Schedule a nightly purge with @Scheduled(cron=\"0 0 2 * * *\") that removes "
                + "entries whose associated token expiry timestamp has passed."
            ),
            new IdentifiedBottleneck(
                "MEDIUM", "MULTI_QUERY",
                "OrderServiceImpl.getStats()",
                "getStats() issues two separate aggregate queries: orderRepository.getStatsByStatus() "
                + "and orderRepository.sumPaidRevenue(). These two round-trips could be combined "
                + "into a single query.",
                "Combine into one native SQL query that returns both status-group counts and the "
                + "paid revenue sum in a single round-trip, reducing latency by ~50% for the "
                + "stats endpoint."
            ),
            new IdentifiedBottleneck(
                "LOW", "CACHE_EVICTION",
                "UserServiceImpl.create()",
                "@CacheEvict(value=\"users\", allEntries=true) on user registration flushes all "
                + "cached user entries. In a write-heavy registration flow this degrades cache "
                + "effectiveness for all concurrent user lookups.",
                "Change to @CacheEvict(key=\"#result.userId\") on create and update, so only "
                + "the affected user entry is invalidated."
            )
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

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
            cpuUsagePct = Math.round(sunOsBean.getProcessCpuLoad() * 10000.0) / 100.0;
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
            // Statistics not available (generate_statistics=false or non-Hibernate provider)
            return new HibernateStats(0L, 0L, "statistics-unavailable", 0L);
        }
    }

    private List<ServiceMethodStat> captureTopSlowMethods() {
        return monitoringAspect.getMetrics().values().stream()
                .map(m -> new ServiceMethodStat(
                        m.getMethodKey(),
                        m.getInvocations(),
                        m.getSlowInvocations(),
                        Math.round(m.getAvgTimeMs() * 10.0) / 10.0,
                        m.getMinTimeMs(),
                        m.getMaxTimeMs()
                ))
                .sorted(Comparator.comparingDouble(ServiceMethodStat::getAvgTimeMs).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private Map<String, CacheStatsSummary> captureCacheStats() {
        Map<String, CacheStatsSummary> result = new LinkedHashMap<>();
        cacheManager.getCacheNames().forEach(name -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache instanceof CaffeineCache caffeineCache) {
                CacheStats stats = caffeineCache.getNativeCache().stats();
                result.put(name, new CacheStatsSummary(
                        stats.hitCount(),
                        stats.missCount(),
                        Math.round(stats.hitRate() * 1000.0) / 1000.0,
                        stats.evictionCount(),
                        caffeineCache.getNativeCache().estimatedSize()
                ));
            }
        });
        return result;
    }

    private List<HttpEndpointStat> captureHttpStats() {
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();
        if (timers == null || timers.isEmpty()) return Collections.emptyList();

        return timers.stream()
                .map(t -> new HttpEndpointStat(
                        t.getId().getTag("uri"),
                        t.getId().getTag("method"),
                        (long) t.count(),
                        Math.round(t.mean(TimeUnit.MILLISECONDS) * 10.0) / 10.0,
                        Math.round(t.max(TimeUnit.MILLISECONDS)  * 10.0) / 10.0
                ))
                .filter(s -> s.getRequestCount() > 0)
                .sorted(Comparator.comparingDouble(HttpEndpointStat::getMeanMs).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
}
