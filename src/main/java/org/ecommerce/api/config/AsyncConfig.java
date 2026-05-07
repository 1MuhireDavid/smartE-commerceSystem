package org.ecommerce.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures the shared async executor used by @Async service methods and
 * CompletableFuture.supplyAsync() calls throughout the application.
 *
 * Thread pool sizing rationale:
 *   corePoolSize  = 4  — keep alive for sustained bursts (1 per typical CPU core)
 *   maxPoolSize   = 16 — handles spikes without exhausting DB connection pool
 *   queueCapacity = 200 — absorbs brief load spikes before rejecting
 *   CallerRunsPolicy — applies backpressure: if queue is full the calling thread
 *     runs the task itself, slowing the producer rather than dropping work
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EXECUTOR_BEAN = "ecommerceTaskExecutor";

    @Bean(EXECUTOR_BEAN)
    public Executor ecommerceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ecom-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
