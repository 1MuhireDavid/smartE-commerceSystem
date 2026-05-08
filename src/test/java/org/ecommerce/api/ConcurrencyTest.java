package org.ecommerce.api;

import org.ecommerce.api.aspect.MethodMetrics;
import org.ecommerce.api.security.TokenBlacklistService;
import org.ecommerce.api.service.impl.RateLimitServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrent correctness tests — Epic 3 (US 3.1).
 *
 * Each test spins up N threads that all start simultaneously via a CountDownLatch
 * and exercises a shared resource. No Spring context is needed; the services under
 * test are plain POJOs with no JPA dependencies.
 */
class ConcurrencyTest {

    // ── TokenBlacklistService ─────────────────────────────────────────────────

    /**
     * 50 threads concurrently revoke distinct tokens and check their own token.
     * Verifies no deadlock, no lost update, and that the audit log size matches
     * the number of revocations.
     */
    @Test
    void tokenBlacklistConcurrentRevokeAndCheck() throws InterruptedException {
        TokenBlacklistService service = new TokenBlacklistService();
        int threads = 50;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final String token = "tok-" + i;
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    service.revoke(token, Instant.now().plusSeconds(60));
                    assertTrue(service.isRevoked(token),
                            "token should be revoked immediately after revoke()");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        ready.await();
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "threads did not finish in time");

        assertEquals(threads, service.getAuditLog().size(),
                "audit log must record every revocation exactly once");
    }

    /**
     * Tokens revoked with a past expiry must not appear as revoked after isRevoked()
     * triggers the lazy eviction inside compute().
     */
    @Test
    void tokenBlacklistExpiredTokensEvictedAtomically() throws InterruptedException {
        TokenBlacklistService service = new TokenBlacklistService();
        int threads = 40;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        AtomicInteger falsePositives = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final String token = "exp-" + i;
            // Revoke with expiry already in the past
            service.revoke(token, Instant.now().minusSeconds(1));
            new Thread(() -> {
                try {
                    start.await();
                    if (service.isRevoked(token)) {
                        falsePositives.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertEquals(0, falsePositives.get(),
                "expired tokens must never be reported as revoked");
    }

    // ── RateLimitService ──────────────────────────────────────────────────────

    /**
     * 30 threads all call isAllowed("user-1") concurrently.
     * Exactly MAX_REQUESTS (10) must be granted; the remaining 20 must be denied.
     */
    @Test
    void rateLimitEnforcedExactlyUnderConcurrentLoad() throws InterruptedException {
        RateLimitServiceImpl service = new RateLimitServiceImpl();
        int threads = 30;
        CountDownLatch start   = new CountDownLatch(1);
        CountDownLatch done    = new CountDownLatch(threads);
        AtomicInteger allowed  = new AtomicInteger(0);
        AtomicInteger denied   = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (service.isAllowed("user-1")) {
                        allowed.incrementAndGet();
                    } else {
                        denied.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(RateLimitServiceImpl.MAX_REQUESTS, allowed.get(),
                "exactly MAX_REQUESTS threads must be allowed");
        assertEquals(threads - RateLimitServiceImpl.MAX_REQUESTS, denied.get(),
                "remaining threads must be denied");
        assertEquals(denied.get(), service.getRecentViolations().size(),
                "each denied request must produce one violation entry");
    }

    /**
     * Independent keys must not interfere: each of 5 keys gets its own quota.
     */
    @Test
    void rateLimitIsolatesKeysConcurrently() throws InterruptedException {
        RateLimitServiceImpl service = new RateLimitServiceImpl();
        int keys    = 5;
        int threads = keys * RateLimitServiceImpl.MAX_REQUESTS;  // exactly fills each quota
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final String key = "key-" + (i % keys);
            pool.submit(() -> {
                try {
                    start.await();
                    if (service.isAllowed(key)) {
                        allowed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(threads, allowed.get(),
                "all requests must be allowed when each key is within its quota");
    }

    // ── MethodMetrics ─────────────────────────────────────────────────────────

    /**
     * 50 threads each record 200 invocations concurrently.
     * Final invocation count must equal 50 × 200 = 10,000 with no lost updates.
     * Min ≤ avg ≤ max must hold after all updates.
     */
    @Test
    void methodMetricsCountIsAccurateUnderLoad() throws InterruptedException {
        MethodMetrics metrics = new MethodMetrics("TestService.doWork");
        int threads          = 50;
        int recordsPerThread = 200;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < recordsPerThread; j++) {
                        metrics.record(j % 100, j % 100 > 90);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(15, TimeUnit.SECONDS));
        pool.shutdown();

        long expected = (long) threads * recordsPerThread;
        assertEquals(expected, metrics.getInvocations(),
                "no invocation must be lost under concurrent record() calls");
        assertTrue(metrics.getMinTimeMs() <= metrics.getAvgTimeMs(),
                "min must be <= avg");
        assertTrue(metrics.getAvgTimeMs() <= metrics.getMaxTimeMs(),
                "avg must be <= max");
        assertEquals(0, metrics.getMinTimeMs(),    // j%100 produces 0 for j=0,100,...
                "min must be 0 (first recorded value in each thread's loop)");
        assertEquals(99, metrics.getMaxTimeMs(),
                "max must be 99 (highest value in j%100 sequence)");
    }
}
