package com.github.instaer.cdc.flinksql.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
@ConfigurationProperties(prefix = "flink.source.connector")
public class FlinkSourceConnectorOptions {

    /**
     * starting value when automatically generating server-id (mysql-cdc only)
     */
    private Long serverIdStart;

    /**
     * Comma-separated server-ids that need to be excluded
     * when automatically generating server-id (mysql-cdc only)
     */
    private String serverIdExclude;

    private Map<String, Map<String, String>> options;
}