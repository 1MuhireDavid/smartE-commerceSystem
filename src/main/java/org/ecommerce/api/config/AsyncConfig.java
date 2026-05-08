package org.ecommerce.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures the shared async executor — Epic 2 (US 2.1) / Epic 3 (US 3.2).
 *
 * Pool sizing is driven by application.yml so profiles can tune it without a code change
 * (see async.executor.* properties).  The bean is typed as ThreadPoolTaskExecutor (not
 * just Executor) so MonitoringController can query live pool stats (active threads,
 * queue depth, completed tasks) via the full API.
 *
 * Three configurations tested and documented in docs/concurrency-thread-safety-report.md:
 *   LOW      — core=2,  max=4,  queue=50   (under-provisioned baseline)
 *   BALANCED — core=4,  max=16, queue=200  (default; matches a 4-core host)
 *   HIGH     — core=8,  max=32, queue=500  (CPU-bound spike handling)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EXECUTOR_BEAN = "ecommerceTaskExecutor";

    @Value("${async.executor.core-pool-size:4}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:16}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:200}")
    private int queueCapacity;

    @Bean(EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor ecommerceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ecom-async-");
        // CallerRunsPolicy: when the queue is full the calling thread runs the task itself,
        // applying natural back-pressure instead of dropping work.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
