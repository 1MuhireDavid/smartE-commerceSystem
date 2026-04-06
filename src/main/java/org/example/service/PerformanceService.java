package org.example.service;

import org.example.dao.ProductDAO;
import org.example.model.Product;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Measures and compares query performance before and after applying
 * in-memory caching and database indexes.
 *
 * <p>Methodology:
 * <ol>
 *   <li>Run N identical search queries against the database (no cache) and
 *       record the total and average elapsed time.</li>
 *   <li>Warm the cache with the first result, then run the same queries
 *       from cache and record elapsed time.</li>
 *   <li>Drop the B-tree name index, run a search, record time.</li>
 *   <li>Re-create the index, run the same search, record time.</li>
 * </ol>
 */
public class PerformanceService {

    private static final int ITERATIONS = 50;

    private final ProductDAO     productDAO     = new ProductDAO();
    private final ProductService productService = new ProductService();

    public record BenchmarkResult(
        String label,
        long   totalMs,
        double avgMs,
        int    resultCount
    ) {}

    /** Run the full benchmark suite and return results for display. */
    public List<BenchmarkResult> runAll(String searchKeyword) throws SQLException {
        List<BenchmarkResult> results = new ArrayList<>();

        // 1. Without cache — raw DB queries
        productService.clearCache();
        long start = System.currentTimeMillis();
        List<Product> lastResult = null;
        for (int i = 0; i < ITERATIONS; i++) {
            lastResult = productDAO.search(searchKeyword, 0);
        }
        long withoutCacheMs = System.currentTimeMillis() - start;
        results.add(new BenchmarkResult(
            "DB query (no cache)",
            withoutCacheMs,
            (double) withoutCacheMs / ITERATIONS,
            lastResult == null ? 0 : lastResult.size()
        ));

        // 2. With cache — first call hits DB, remaining serve from cache
        productService.clearCache();
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            productService.search(searchKeyword, 0);
        }
        long withCacheMs = System.currentTimeMillis() - start;
        results.add(new BenchmarkResult(
            "Cached query",
            withCacheMs,
            (double) withCacheMs / ITERATIONS,
            productService.search(searchKeyword, 0).size()
        ));

        // 3. Without index — drop the B-tree name index then search
        productDAO.dropNameIndex();
        start = System.currentTimeMillis();
        lastResult = productDAO.search(searchKeyword, 0);
        long withoutIndexMs = System.currentTimeMillis() - start;
        results.add(new BenchmarkResult(
            "Search without index",
            withoutIndexMs,
            withoutIndexMs,          // single run
            lastResult.size()
        ));

        // 4. With index — re-create the index then search
        productDAO.createNameIndex();
        start = System.currentTimeMillis();
        lastResult = productDAO.search(searchKeyword, 0);
        long withIndexMs = System.currentTimeMillis() - start;
        results.add(new BenchmarkResult(
            "Search with index",
            withIndexMs,
            withIndexMs,             // single run
            lastResult.size()
        ));

        return results;
    }

    /** Produce a human-readable summary comparing the benchmark results. */
    public static String summarise(List<BenchmarkResult> results) {
        if (results.size() < 4) return "Insufficient results.";

        BenchmarkResult noCache    = results.get(0);
        BenchmarkResult withCache  = results.get(1);
        BenchmarkResult noIndex    = results.get(2);
        BenchmarkResult withIndex  = results.get(3);

        double cacheSpeedup = noCache.avgMs() > 0
            ? noCache.avgMs() / Math.max(withCache.avgMs(), 0.01)
            : 0;
        double indexSpeedup = noIndex.totalMs() > 0
            ? (double) noIndex.totalMs() / Math.max(withIndex.totalMs(), 1)
            : 0;

        return String.format("""
            ═══════════════════════════════════════════════════════
              Smart E-Commerce — Performance Benchmark Report
            ═══════════════════════════════════════════════════════
            Iterations (cache test): %d

            [Cache Benchmark]
              Without cache  — avg per query : %.3f ms  (total %d ms)
              With cache     — avg per query : %.3f ms  (total %d ms)
              Cache speedup  : %.1fx faster

            [Index Benchmark — single query each]
              Without index  : %d ms
              With index     : %d ms
              Index speedup  : %.1fx faster

            [Conclusion]
              In-memory caching reduces average query latency by ~%.0f%%.
              The B-tree index on lower(name) reduces name-search latency by ~%.0f%%.
            ═══════════════════════════════════════════════════════
            """,
            ITERATIONS,
            noCache.avgMs(),   noCache.totalMs(),
            withCache.avgMs(), withCache.totalMs(),
            cacheSpeedup,
            noIndex.totalMs(),
            withIndex.totalMs(),
            indexSpeedup,
            Math.max(0, (1 - 1.0 / cacheSpeedup) * 100),
            Math.max(0, (1 - 1.0 / indexSpeedup) * 100)
        );
    }
}
