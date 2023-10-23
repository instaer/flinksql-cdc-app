package com.github.instaer.cdc.flinksql.service;

import com.github.instaer.cdc.flinksql.property.FlinkEnvironment;
import com.github.instaer.cdc.flinksql.property.FlinkSinkConnectorOptions;
import com.github.instaer.cdc.flinksql.property.FlinkSourceConnectorOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReadFlinkSQLService implements ApplicationRunner {

    @Autowired
    private FlinkSourceConnectorOptions sourceConnectorOptions;

    @Autowired
    private FlinkSinkConnectorOptions sinkConnectorOptions;

    @Autowired
    private FlinkEnvironment flinkEnvironment;

    @Autowired
    private ExecuteFlinkSQLService executeFlinkSQLService;

    @Autowired
    private ThreadPoolTaskExecutor jobExecutor;

    @Value("${log.connector.options.password.hide:true}")
    private boolean hidePasswordOptions;

    private static File[] files = null;
    private static final String VARIABLES_PREFIX = "${";
    private static final String VARIABLES_SUFFIX = "}";
    private static final PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private static final HashMap<String, Long> serverIdMap = new HashMap<>();
    private static final Pattern hostnamePattern = Pattern.compile("'hostname'\\s*=\\s*'([^']+)'");
    private static final Pattern portPattern = Pattern.compile("'port'\\s*=\\s*'([^']+)'");
    private static final Pattern passwordPattern = Pattern.compile("'password'\\s*=\\s*'([^']+)'");

    @SneakyThrows
    @PostConstruct
    private void init() {
        Resource[] resources = resourcePatternResolver.getResources("classpath:sql/*.{[sS][qQ][lL]}");
        int sqlSum = resources.length;
        if (sqlSum > 0) {
            files = new File[sqlSum];
            for (int i = 0; i < resources.length; i++) {
                files[i] = resources[i].getFile();
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        for (File file : files) {
            jobExecutor.execute(() -> {
                try {
                    List<String> segmentSQLs = read(file);
                    executeFlinkSQLService.execute(FilenameUtils.getBaseName(file.getName()), segmentSQLs);
                } catch (Exception e) {
                    log.error("Read file:" + file.getName() + " failed, ", e);
                }
            });
        }
    }

    @SneakyThrows
    private List<String> read(File file) {
        List<String> segmentSQLs = new ArrayList<>();
        List<String> lines = Files.lines(file.toPath()).collect(Collectors.toList());
        List<String> segment = new ArrayList<>();
        Map<String, String> connectorOption = null;
        boolean containsMySqlCdc = false;
        String hostname = null;
        String port = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }

            int commentSymbolIndex = line.indexOf("--");
            if (commentSymbolIndex > -1) {
                line = line.substring(0, commentSymbolIndex).trim();
            }

            if (line.startsWith(VARIABLES_PREFIX) && line.contains(VARIABLES_SUFFIX)) {
                if (!line.endsWith(",")) {
                    throw new RuntimeException("Lines containing variables are not allowed to appear in the last line of connector options. The error occurs at this line:" + line);
                }

                String variablesName = StringUtils.substringBetween(line, VARIABLES_PREFIX, VARIABLES_SUFFIX);
                segment.addAll(replaceVariables(variablesName));
                connectorOption = sourceConnectorOptions.getOptions().get(variablesName);
                continue;
            }
            else if (line.startsWith("'connector'") && line.contains("mysql-cdc")) {
                containsMySqlCdc = true;
            }
            else if (line.startsWith("'hostname'")) {
                Matcher hostnameMatcher = hostnamePattern.matcher(line);
                if (hostnameMatcher.find()) {
                    hostname = hostnameMatcher.group(1);
                }
            }
            else if (line.startsWith("'port'")) {
                Matcher portMatcher = portPattern.matcher(line);
                if (portMatcher.find()) {
                    port = portMatcher.group(1);
                }
            }
            else if (line.contains(";")) {
                if (line.indexOf(";") != line.lastIndexOf(";") || line.indexOf(";") != line.length() - 1) {
                    throw new RuntimeException("The end line of the SQL statement must end with a semicolon, and no other sql is allowed to be added. The error occurs at this line:" + line);
                }

                if (containsMySqlCdc) {
                    if (null != connectorOption) {
                        hostname = connectorOption.get("hostname");
                        port = connectorOption.get("port");
                    }

                    if (null != hostname && null != port) {
                        String lastSegment = segment.get(segment.size() - 1);
                        if (!lastSegment.endsWith(",")) {
                            segment.set(segment.size() - 1, lastSegment + ",");
                        }

                        String serverId = calculateServerId(hostname, port);
                        segment.add("'server-id' = '" + serverId + "'");
                    }

                    connectorOption = null;
                    hostname = null;
                    port = null;
                    containsMySqlCdc = false;
                }

                segment.add(line);
                StringJoiner lineJoiner = new StringJoiner(System.lineSeparator());
                segment.forEach(lineJoiner::add);
                segment.clear();

                String segmentSQL = lineJoiner.toString();
                segmentSQLs.add(segmentSQL);

                if (hidePasswordOptions) {
                    Matcher passwordMatcher = passwordPattern.matcher(segmentSQL);
                    if (passwordMatcher.find()) {
                        segmentSQL = segmentSQL.replace("'" + passwordMatcher.group(1) + "'", "******");
                    }
                }

                log.info("Reading sql from file ({}):{}", file.getName(), segmentSQL);
                continue;
            }

            segment.add(line);
        }

        return segmentSQLs;
    }

    private List<String> replaceVariables(String variablesName) {
        List<String> optionLines = new ArrayList<>();
        Map<String, String> connectorOption = sourceConnectorOptions.getOptions()
                .get(variablesName);
        if (null == connectorOption) {
            connectorOption = sinkConnectorOptions.getOptions().get(variablesName);
            if (null == connectorOption) {
                throw new RuntimeException("Unknown variables:'" + variablesName + "', check your configuration");
            }
        }

        connectorOption.forEach((k, v) -> optionLines.add("'" + k + "' = '" + v + "',"));
        return optionLines;
    }

    /**
     * calculate server-id for mysql-cdc
     *
     * @param hostName
     * @param port
     * @return
     */
    private synchronized String calculateServerId(String hostName, String port) {
        String serverKey = hostName + ":" + port;
        String serverIdExclude = sourceConnectorOptions.getServerIdExclude();
        List<String> excludeServerIds = new ArrayList<>();
        if (null != serverIdExclude) {
            excludeServerIds.addAll(Arrays.asList(serverIdExclude.split(",")));
        }

        Long serverId = serverIdMap.get(serverKey);
        if (null == serverId) {
            Long serverIdStart = sourceConnectorOptions.getServerIdStart();
            while (excludeServerIds.contains(serverIdStart.toString())) {
                serverIdStart++;
            }
            serverId = serverIdStart;
        }
        else {
            while (excludeServerIds.contains(serverId.toString())) {
                serverId++;
            }
            serverId++;
        }

        int parallelism = flinkEnvironment.getDefaultParallelism();
        if (parallelism < 2) {
            serverIdMap.put(serverKey, serverId);
            return serverId.toString();
        }

        long serverIdRangeRight = serverId + parallelism;
        while (excludeServerIds.contains(String.valueOf(serverIdRangeRight))) {
            serverIdRangeRight++;
        }

        serverIdMap.put(serverKey, serverIdRangeRight);
        return serverId + "-" + (serverIdRangeRight);
    }
}