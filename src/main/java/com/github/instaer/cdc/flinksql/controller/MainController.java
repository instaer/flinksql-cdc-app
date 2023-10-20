package com.github.instaer.cdc.flinksql.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.instaer.cdc.flinksql.property.FlinkSinkConnectorOptions;
import com.github.instaer.cdc.flinksql.property.FlinkSourceConnectorOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
public class MainController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlinkSourceConnectorOptions flinkSourceConnectorOptions;

    @Autowired
    private FlinkSinkConnectorOptions flinkSinkConnectorOptions;

    @Autowired
    private ThreadPoolTaskExecutor jobExecutor;

    @SneakyThrows
    @GetMapping("/connectorOptions")
    public String connectorOptions() {
        Map<String, Map<String, String>> sourceConnectorOptions = new HashMap<>(flinkSourceConnectorOptions.getOptions());
        Map<String, Map<String, String>> sinkConnectorOptions = new HashMap<>(flinkSinkConnectorOptions.getOptions());
        Arrays.asList(sourceConnectorOptions, sinkConnectorOptions).forEach(m ->
                m.replaceAll((k, v) -> {
                    Map<String, String> newV = new HashMap<>(v);
                    newV.replaceAll((ck, cv) -> ck.contains("password") ? "******" : cv);
                    return newV;
                })
        );

        FlinkSourceConnectorOptions printFlinkSourceConnectorOptions = FlinkSourceConnectorOptions.builder()
                .serverIdStart(flinkSourceConnectorOptions.getServerIdStart())
                .serverIdExclude(flinkSourceConnectorOptions.getServerIdExclude())
                .options(sourceConnectorOptions)
                .build();
        FlinkSinkConnectorOptions printFlinkSinkConnectorOptions = FlinkSinkConnectorOptions.builder()
                .options(sinkConnectorOptions)
                .build();
        Map<String, Object> map = new HashMap<>(2);
        map.put("flinkSourceConnectorOptions", printFlinkSourceConnectorOptions);
        map.put("flinkSinkConnectorOptions", printFlinkSinkConnectorOptions);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }

    @SneakyThrows
    @GetMapping("/jobExecutor")
    public String jobExecutor() {
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