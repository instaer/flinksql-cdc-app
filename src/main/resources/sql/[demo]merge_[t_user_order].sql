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