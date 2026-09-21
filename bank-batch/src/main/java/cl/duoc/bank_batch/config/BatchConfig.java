package cl.duoc.bank_batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchConfig {

    @Value("${batch.threads.core:3}")
    private int corePoolSize;

    @Value("${batch.threads.max:3}")
    private int maxPoolSize;

    @Value("${batch.queue.capacity:25}")
    private int queueCapacity;

    @Bean
    public AsyncTaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("Batch-Thread-");

        executor.initialize();

        System.out.println(
                "CONFIGURACION BATCH -> Threads core: "
                        + corePoolSize
                        + " | Threads max: "
                        + maxPoolSize
                        + " | Queue: "
                        + queueCapacity
        );

        return executor;
    }
}