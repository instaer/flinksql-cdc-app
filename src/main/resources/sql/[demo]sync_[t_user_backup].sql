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
     'scan.startup.mode' = 'latest-offset'
);

CREATE TABLE t_user_backup
WITH (
    'connector' = 'jdbc',
    ${dbserver2},
    'table-name' = 't_user_backup'
)
LIKE t_user (
    EXCLUDING OPTIONS
);

CREATE TABLE t_user_backup_copy
WITH (
    'connector' = 'jdbc',
    ${dbserver2},
    'table-name' = 't_user_backup_copy'
)
LIKE t_user (
    EXCLUDING OPTIONS
);

INSERT INTO t_user_backup
SELECT id,
       name,
       email,
       create_time
FROM t_user;
INSERT INTO t_user_backup_copy
SELECT id,
       name,
       email,
       create_time
FROM t_user;