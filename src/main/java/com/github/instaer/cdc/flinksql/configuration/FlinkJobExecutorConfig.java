package com.github.instaer.cdc.flinksql.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class FlinkJobExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor jobExecutor() {
        ThreadPoolTaskExecutor jobExecutor = new ThreadPoolTaskExecutor();
        jobExecutor.setCorePoolSize(0);
        jobExecutor.setMaxPoolSize(Integer.MAX_VALUE);
        jobExecutor.setKeepAliveSeconds(60);
        jobExecutor.setQueueCapacity(0);
        jobExecutor.setThreadNamePrefix("flink-job-executor-");
        jobExecutor.initialize();
        return jobExecutor;
    }
}