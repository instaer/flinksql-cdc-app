package com.github.instaer.cdc.flinksql.service;

import com.github.instaer.cdc.flinksql.property.FlinkConfiguration;
import com.github.instaer.cdc.flinksql.property.FlinkEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.minicluster.MiniCluster;
import org.apache.flink.runtime.minicluster.MiniClusterConfiguration;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExecuteFlinkSQLService {

    @Autowired
    private FlinkConfiguration flinkConfiguration;

    @Autowired
    private FlinkEnvironment flinkEnvironment;

    @SneakyThrows
    @PostConstruct
    private void init() {
        Configuration configuration = new Configuration();
        configuration.setInteger(RestOptions.PORT, flinkConfiguration.getRestPort());
        MiniCluster miniCluster = new MiniCluster(new MiniClusterConfiguration.Builder()
                .setConfiguration(configuration)
                .setNumTaskManagers(flinkConfiguration.getLocalNumberTaskmanager())
                .setNumSlotsPerTaskManager(flinkConfiguration.getNumTaskSlots())
                .build());
        miniCluster.start();
    }

    public void execute(String jobName, List<String> segmentSQLs) {
        try (StreamExecutionEnvironment env = StreamExecutionEnvironment.createRemoteEnvironment("localhost", flinkConfiguration.getRestPort())) {
            env.enableCheckpointing(flinkEnvironment.getCheckpointInterval());
            env.getCheckpointConfig().setCheckpointTimeout(flinkEnvironment.getCheckpointTimeout());
            env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(30, TimeUnit.SECONDS)));

            env.setStateBackend(new HashMapStateBackend());
            env.getCheckpointConfig().setCheckpointStorage(flinkEnvironment.getCheckpointStorage());
            env.getCheckpointConfig().setTolerableCheckpointFailureNumber(flinkEnvironment.getCheckpointFailureNumber());
            env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

            StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
            tableEnv.getConfig().set(PipelineOptions.NAME, jobName);
            tableEnv.getConfig().set(CoreOptions.DEFAULT_PARALLELISM, flinkEnvironment.getDefaultParallelism());
            tableEnv.getConfig().set(ExecutionCheckpointingOptions.ENABLE_CHECKPOINTS_AFTER_TASKS_FINISH, true);

            // 根据INSERT语句数量判断是否调用StatementSet对象执行
            Predicate<String> isInsertSQL = sql -> "INSERT".equalsIgnoreCase(sql.substring(0, sql.indexOf(" ")));
            Map<Boolean, List<String>> sqlGroup = segmentSQLs.stream().collect(Collectors.partitioningBy(isInsertSQL));
            sqlGroup.get(Boolean.FALSE).forEach(sql -> tableEnv.executeSql(sql).print());

            List<String> insertSQLs = sqlGroup.get(Boolean.TRUE);
            if (!insertSQLs.isEmpty()) {
                if (insertSQLs.size() == 1) {
                    tableEnv.executeSql(insertSQLs.get(0)).print();
                }
                else {
                    StreamStatementSet statementSet = tableEnv.createStatementSet();
                    insertSQLs.forEach(statementSet::addInsertSql);
                    statementSet.execute().print();
                }
            }

            env.executeAsync();
        } catch (Exception e) {
            log.error("Execute job(" + jobName + ") failed", e);
        }
    }
}