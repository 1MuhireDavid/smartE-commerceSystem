# Performance Baseline Report — SmartE-commerceSystem

**Epic 1 · User Stories 1.1 & 1.2**  
Analysis date: 2026-05-10  
Branch: `feat/advancedOptimization`

---

## 1. Executive Summary

This report documents the performance baseline of the secured SmartE-commerceSystem backend
before any async or structural optimisations are applied. It identifies where the system spends
time, which database access patterns are inefficient, and what concrete improvements are
recommended for subsequent Epics.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL · Spring Data JPA/Hibernate · Caffeine cache ·
Spring Security (JWT + OAuth2)

**Outcome:** 13 bottlenecks identified across four categories — N+1 queries, missing indexes,
cache inefficiencies, and blocking synchronous operations. All are covered by live-observable
metrics exposed at `/api/monitoring/performance-report`.

---

## 2. Profiling Tools Used

| Tool | How Used | Endpoint / API |
|---|---|---|
| **AOP PerformanceMonitoringAspect** | `@Around` advice wraps every `service.impl.*` method; records invocation count, avg/min/max/last time, slow-call count | `GET /api/monitoring/metrics` |
| **Micrometer / Spring Boot Actuator** | Auto-collects `http.server.requests` Timer per URI+method+status combination | queried via `MeterRegistry` in `PerformanceReportServiceImpl` |
| **Hibernate Statistics** | `SessionFactory.getStatistics()` — total query count, max query time, slowest query string, entity load count. Enabled via `hibernate.generate_statistics=true` | `GET /api/monitoring/performance-report` → `hibernateStats` section |
| **JMX MBeans** | `MemoryMXBean` (heap/non-heap), `OperatingSystemMXBean` (process CPU load), `ThreadMXBean` (thread count), `GarbageCollectorMXBean` (GC count + time) | `GET /api/monitoring/performance-report` → `jvm` section |
| **Caffeine recordStats** | Built-in hit/miss/eviction tracking on all three caches (`products`, `categories`, `users`) | `GET /api/monitoring/cache-stats` |

### Using VisualVM or Java Flight Recorder (optional)

To get a deeper CPU/memory flame graph:

```bash
# Start the app with JFR enabled (Java 21)
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -jar target/SmartE-commerceSystem-1.0-SNAPSHOT.jar

# Open recording.jfr in VisualVM (File → Load) or JDK Mission Control
```

For VisualVM live monitoring: connect to PID via `jvisualvm`, attach to the running process,
and run the Sampler while exercising the API with Postman.

---

## 3. Baseline JVM Metrics

> Capture these by calling `GET /api/monitoring/performance-report` after exercising the API
> with a representative set of requests. Replace the placeholder values with actual data.

| Metric | Baseline Value | Notes |
|---|---|---|
| Heap used (MB) | _(capture post-run)_ | From `MemoryMXBean.getHeapMemoryUsage()` |
| Heap max (MB) | _(capture post-run)_ | JVM `-Xmx` default ~256 MB |
| Heap usage % | _(capture post-run)_ | Alert if > 80% |
| Non-heap used (MB) | _(capture post-run)_ | Metaspace + code cache |
| Process CPU % | _(capture post-run)_ | 0.0–1.0 range from `OperatingSystemMXBean` |
| Thread count | _(capture post-run)_ | Spring Boot default pool ~200 |
| GC total count | _(capture post-run)_ | Sum across all collectors |
| GC total time (ms) | _(capture post-run)_ | High value indicates GC pressure |

---

## 4. Baseline Hibernate Query Statistics

> Capture after making ~20 diverse API calls (product search, cart, orders, reviews).

| Metric | Baseline Value | Notes |
|---|---|---|
| Total query executions | _(capture post-run)_ | Includes all JPQL + native SQL |
| Max single query time (ms) | _(capture post-run)_ | Slowest individual query |
| Slowest query string | _(capture post-run)_ | Indicates worst DB access path |
| Entity load count | _(capture post-run)_ | High value may indicate N+1 |

**Expected finding:** After a `GET /api/cart/{id}/items` call the entity load count will be
significantly higher than the query count — the telltale sign of N+1 lazy-loading.

---

## 5. Baseline Service Method Timing (Top 10 Slowest)

> Capture from `GET /api/monitoring/metrics` or the `topSlowMethods` section of the report.

| Method | Avg (ms) | Min (ms) | Max (ms) | Slow calls | Notes |
|---|---|---|---|---|---|
| `OrderServiceImpl.create` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | N sequential inserts |
| `ReviewServiceImpl.findAll` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | Triple lazy-load |
| `CartServiceImpl.getItems` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | N+1 on product |
| `OrderServiceImpl.findItems` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | N+1 on product |
| `ProductServiceImpl.findAll` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | Native FTS, uncached |
| `PaymentServiceImpl.findAll` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | Lazy order load |
| `OrderServiceImpl.getStats` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | 2 round-trips |
| _(remaining)_ | | | | | |

Slow-call threshold: **500 ms** (configurable via `monitoring.slow-method-threshold-ms`).

---

## 6. Baseline HTTP Endpoint Latency (Top 10 Slowest)

> Capture from `slowestEndpoints` in the performance report after load testing.

| URI | Method | Requests | Mean (ms) | Max (ms) |
|---|---|---|---|---|
| _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ |

---

## 7. Baseline Cache Statistics

| Cache | Hit count | Miss count | Hit rate | Evictions | Size |
|---|---|---|---|---|---|
| `products` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ |
| `categories` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ |
| `users` | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ | _(capture)_ |

---

## 8. Identified Bottlenecks

All 13 bottlenecks are returned by `GET /api/monitoring/bottlenecks`. The table below is the
static analysis summary.

| # | Severity | Category | Location | Description | Recommendation |
|---|---|---|---|---|---|
| 1 | **CRITICAL** | N+1_QUERY | `OrderServiceImpl.findItems()` | `findByOrder_OrderId()` returns items without JOIN FETCH on product; N lazy-load SELECT queries fire per item | Add `JOIN FETCH` or `@EntityGraph` for product in `OrderItemRepository` |
| 2 | **CRITICAL** | N+1_QUERY | `ReviewServiceImpl.findAll()` | Three lazy associations (product, user, order) = up to 3N extra queries per page | Add `JOIN FETCH` for all three in `ReviewRepository.search()` |
| 3 | **CRITICAL** | N+1_QUERY | `CartServiceImpl.getItems()` | Lazy `product` on each `CartItemEntity`; N SELECT queries when caller accesses price/name | Add `JOIN FETCH cart_item.product` query in `CartItemRepository` |
| 4 | **CRITICAL** | BLOCKING_LOOP | `OrderServiceImpl.create()` | N product lookups + N item inserts executed sequentially in a loop | Use `findAllById()` + `saveAll()` for batch loading and inserting |
| 5 | **CRITICAL** | MISSING_INDEX | `categories` table | No index on `name` or `is_active`; every filter is a full-table scan | `CREATE INDEX idx_categories_name ON categories (lower(name))` |
| 6 | **HIGH** | CACHE_EVICTION | `ProductServiceImpl.create()` | `@CacheEvict(allEntries=true)` flushes entire products cache on every new product | Change to per-key eviction |
| 7 | **HIGH** | UNCACHED_SEARCH | `ProductServiceImpl.findAll()` | Native FTS query runs on every keyword search with no result caching | Add `@Cacheable` on the search method with a 2-min TTL |
| 8 | **HIGH** | N+1_QUERY | `PaymentServiceImpl.findAll()` | Lazy `order` association; N extra queries when accessing payment.order | Add `JOIN FETCH` for order in `PaymentRepository.search()` |
| 9 | **HIGH** | MISSING_INDEX | `cart_items` table | No index on `cart_id` or `product_id`; cart item lookups are O(n) scans | `CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id)` |
| 10 | **MEDIUM** | MISSING_INDEX | `reviews(product_id, is_approved)` | Filtering approved reviews per product requires full scan on reviews table | `CREATE INDEX idx_reviews_product_approved ON reviews (product_id, is_approved)` |
| 11 | **MEDIUM** | MEMORY_LEAK | `TokenBlacklistService` | Revoked tokens accumulate in a `ConcurrentHashMap` with no cleanup | Add `@Scheduled` purge to remove expired tokens |
| 12 | **MEDIUM** | MULTI_QUERY | `OrderServiceImpl.getStats()` | Two separate aggregate queries where one combined query suffices | Merge into single native SQL returning both status counts and revenue |
| 13 | **LOW** | CACHE_EVICTION | `UserServiceImpl.create()` | `allEntries=true` on registration flushes entire users cache | Change to per-key `@CacheEvict` |

---

## 9. How to Capture a Baseline

### Prerequisites

1. App running on `dev` profile (PostgreSQL + Hibernate stats enabled)
2. Admin JWT token from `POST /api/auth/login`

### Step-by-step

```bash
# 1. Warm up the API — run a few requests across all domains
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/products
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cart/1/items
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/orders
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/reviews

# 2. Capture the full baseline snapshot
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/monitoring/performance-report | jq .

# 3. View just the bottleneck catalogue
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/monitoring/bottlenecks | jq .

# 4. View per-method timing with min/max
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/monitoring/metrics | jq .

# 5. View cache hit rates
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/monitoring/cache-stats | jq .
```

### Postman collection

Import into Postman — all requests require the `Authorization: Bearer {{token}}` header.
Use the `performance-report` response as the before-optimisation snapshot; capture again
after each Epic's changes to measure improvement.

---

## 10. Summary & Next Steps

The system is functionally correct and secure, but all service operations are **synchronous and
blocking** with no async processing. The most impactful quick wins are:

1. Fix N+1 queries in `OrderServiceImpl`, `ReviewServiceImpl`, `CartServiceImpl` — these cause
   response-time degradation proportional to the data size.
2. Add missing indexes on `categories` and `cart_items` — zero-code change, immediate speedup.
3. Switch `OrderServiceImpl.create()` to batch operations — reduces DB round-trips from 2N → 2.

Subsequent Epics will apply `@Async` + `CompletableFuture` for parallelism, connection-pool
tuning, and cache strategy improvements, using this baseline as the comparison point.
