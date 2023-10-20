package com.github.instaer.cdc.flinksql.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flink.environment")
public class FlinkEnvironment {
    private int defaultParallelism;
    private long checkpointInterval;
    private long checkpointTimeout;
    private String checkpointStorage;
    private int checkpointFailureNumber;
}