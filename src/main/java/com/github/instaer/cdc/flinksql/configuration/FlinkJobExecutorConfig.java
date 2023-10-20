package com.github.instaer.cdc.flinksql.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class FlinkJobExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor jobExecutor() {
        ThreadPoolTaskExecutor jobExecutor = new ThreadPoolTaskExecutor();
        jobExecutor.setCorePoolSize(8);
        jobExecutor.setMaxPoolSize(16);
        jobExecutor.setKeepAliveSeconds(60);
        jobExecutor.setQueueCapacity(100);
        jobExecutor.setThreadNamePrefix("flink-job-executor-");
        jobExecutor.initialize();
        return jobExecutor;
    }
}