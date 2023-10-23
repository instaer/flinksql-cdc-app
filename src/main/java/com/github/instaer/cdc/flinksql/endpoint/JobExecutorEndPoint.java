package com.github.instaer.cdc.flinksql.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Endpoint(id = "jobExecutor")
@Component
public class JobExecutorEndPoint {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ThreadPoolTaskExecutor jobExecutor;

    @ReadOperation
    @SneakyThrows
    public String getJobExecutor() {
        Map<String, Object> map = new LinkedHashMap<>(6);
        map.put("CorePoolSize", jobExecutor.getCorePoolSize());
        map.put("MaxPoolSize", jobExecutor.getMaxPoolSize());
        map.put("CurrentPoolSize", jobExecutor.getPoolSize());
        map.put("ActiveThreadsCount", jobExecutor.getActiveCount());
        map.put("KeepAliveSeconds", jobExecutor.getKeepAliveSeconds());
        map.put("TaskQueueSize", jobExecutor.getThreadPoolExecutor().getQueue().size());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }
}