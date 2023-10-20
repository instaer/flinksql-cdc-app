package com.github.instaer.cdc.flinksql.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flink.configuration")
public class FlinkConfiguration {
    private int restPort;
    private int localNumberTaskmanager;
    private int numTaskSlots;
}