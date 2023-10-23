package com.github.instaer.cdc.flinksql.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.instaer.cdc.flinksql.property.FlinkSinkConnectorOptions;
import com.github.instaer.cdc.flinksql.property.FlinkSourceConnectorOptions;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Endpoint(id = "connectorOptions")
@Component
public class ConnectorOptionsEndPoint {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlinkSourceConnectorOptions flinkSourceConnectorOptions;

    @Autowired
    private FlinkSinkConnectorOptions flinkSinkConnectorOptions;

    @ReadOperation
    @SneakyThrows
    public String getConnectorOptions() {
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
}