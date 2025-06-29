package com.juahaki.juahaki.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class DocumentProcessingConfig {

    @Value("${app.document.processing.chunk-size:1000}")
    private int chunkSize;

    @Value("${app.document.processing.chunk-overlap:200}")
    private int chunkOverlap;

    @Value("${app.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${app.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${app.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean
    @Primary
    public TokenTextSplitter tokenTextSplitter() {
        log.info("Configuring TokenTextSplitter with chunk size: {}, overlap: {}",
                chunkSize, chunkOverlap);

        return new TokenTextSplitter(
                chunkSize,
                chunkOverlap,
                5,
                10000,
                true
        );
    }

    /**
     * Configure async executor for document processing
     */
    @Bean("documentProcessingExecutor")
    public Executor documentProcessingExecutor() {
        log.info("Configuring async executor - core: {}, max: {}, queue: {}",
                corePoolSize, maxPoolSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("DocProcessing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    /**
     * Configure async executor for quiz AI operations
     */
    @Bean("quizAIExecutor")
    public Executor quizAIExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("QuizAI-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("Configured quiz AI executor");
        return executor;
    }
}
