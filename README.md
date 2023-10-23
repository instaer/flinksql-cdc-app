# Light Application for FlinkSQL CDC

轻量级的[**Flink CDC**](https://github.com/ververica/flink-cdc-connectors)应用，通过运行在Spring Boot容器中的Flink本地集群进行部署（主要用于测试和简单使用）。

基于Flink CDC连接器的Table/SQL API，可以使用SQL DDL创建CDC Source来捕获单个表的更改。

## Features

* 支持连接器选项动态配置，避免在SQL中直接写入，方便进行重用。对于配置和代码分离的场景下很有必要，例如使用配置中心；
* 支持MySQL连接器server id自动生成（单个整数或整数范围）。MySQL集群中运行的所有slave节点，标记每个节点的server id都必须是唯一的，否则会冲突；
* 支持在脚本中插入数据到多张表。当检测到多个INSERT语句时，自动通过`STATEMENT SET`执行；

## Usage Example

将MySQL数据库`db1`订单表`t_order`关联用户表`t_user`，并将关联后的订单数据写入MySQL数据库`db2`用户订单表`t_user_order`。

1. 添加脚本文件

在`sql`目录下添加脚本文件（参考`[demo]*.sql`示例），例如：`[demo]merge_[t_user_order].sql`，内容如下：

```sql
CREATE TABLE t_user
(
    id          INT,
    name        STRING,
    email       STRING,
    create_time TIMESTAMP,
    PRIMARY KEY (id) NOT ENFORCED
)
WITH (
    'connector' = 'mysql-cdc',
    ${dbserver1},
    'database-name' = 'db1',
    'table-name' = 't_user',
    'server-time-zone' = 'Asia/Shanghai',
    'scan.startup.mode' = 'initial'
);

CREATE TABLE t_order
(
    id          INT,
    user_id     INT,
    order_no    STRING,
    amount      DOUBLE,
    quantity    INT,
    create_time TIMESTAMP,
    PRIMARY KEY (id) NOT ENFORCED
)
WITH (
    'connector' = 'mysql-cdc',
    ${dbserver1},
    'database-name' = 'db1',
    'table-name' = 't_order',
    'server-time-zone' = 'Asia/Shanghai',
    'scan.startup.mode' = 'latest-offset' --从最晚位点启动
);

CREATE TABLE t_user_order
(
    order_no          STRING,
    user_name         STRING,
    user_email        STRING,
    order_amount      DOUBLE,
    order_quantity    INT,
    order_create_time TIMESTAMP,
    PRIMARY KEY (order_no) NOT ENFORCED
)
WITH (
    'connector' = 'jdbc',
    ${dbserver2},
    'table-name' = 't_user_order'
);

INSERT INTO t_user_order
SELECT o.order_no,
       u.name,
       u.email,
       o.amount,
       o.quantity,
       o.create_time
FROM t_order o
LEFT JOIN t_user u ON o.user_id = u.id;
```

上述脚本中的`${dbserver1}`和`${dbserver2}`占位符表示连接器动态配置选项，例如数据库连接配置。

2. 添加连接器配置

在配置文件`application.properties`中添加连接器配置。

* Source Connector配置

```properties
flink.source.connector.options.dbserver1.hostname=127.0.0.1
flink.source.connector.options.dbserver1.port=3306
flink.source.connector.options.dbserver1.username=username
flink.source.connector.options.dbserver1.password=password
```

* Sink Connector配置

```properties
flink.sink.connector.options.dbserver2.url=jdbc:mysql://127.0.0.1:3306/db2
flink.sink.connector.options.dbserver2.username=username
flink.sink.connector.options.dbserver2.password=password
```

上述配置中的`dbserver1`和`dbserver2`唯一的表示一组连接器配置，在sql中可以直接使用`${dbserver1}`和`${dbserver2}`占位符进行导入。

3. 提交任务

应用启动后，将自动解析和执行`sql`目录下的脚本文件，并以脚本名称作为任务名，以脚本为单位提交任务到local模式启动的Flink集群。

任务可以通过Flink Web Dashboard查看，默认访问地址为：`<PROTOCOL>://<IP>:18081`。可通过配置属性`flink.configuration.rest-port`自定义Web UI端口。

任务截图：
![\[demo\]merge_\[t_user_order\]任务截图](https://raw.githubusercontent.com/instaer/static/file/images/%5Bdemo%5Dmerge_%5Bt_user_order%5D%E4%BB%BB%E5%8A%A1%E6%88%AA%E5%9B%BE.PNG)

## Add Dependency

请按需引入数据库驱动依赖及连接器依赖，依赖版本可按照实际环境调整。

项目中默认引入数据库驱动：

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc6</artifactId>
    <version>11.2.0.4</version>
</dependency>
```

默认引入JDBC SQL连接器：

```xml
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-jdbc</artifactId>
    <version>${flink.version}</version>
</dependency>
```

如果需要使用其它类型数据库和连接器，请自行添加。

## Actuator Endpoints Provided

通过`Spring Boot Actuator`模块创建自定义Endpoint，用于实时查看运行Job的线程池状态以及各连接器配置选项，也可以接入服务监控系统（如Prometheus）。

默认以Web方式暴露Endpoint地址为：
* `/actuator/connectorOptions`
* `/actuator/jobExecutor`
