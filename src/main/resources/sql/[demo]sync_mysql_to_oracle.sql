CREATE TABLE `COINSDETAIL`
(
    `ID`                  INT,
    `DETAILNO`            STRING,
    `SERIALNO`            INT,
    `COINSCODE`           STRING,
    `COINSNAME`           STRING,
    `CURRENCY`            STRING,
    `COINSAMOUNT`         DOUBLE,
    `OPERATEFEE`          DOUBLE,
    `FLAG`                STRING,
    `COINSTYPE`           STRING,
    `INSTANCEID`          STRING,
    `FORMCREATEDATE`      TIMESTAMP,
    `FORMUPDATEDATE`      TIMESTAMP,
    `DEFINEID`            STRING,
    `FORMCREATEBYID`      STRING,
    `FORMUPDATEBYID`      STRING,
    `INSERTTEMPTIME`      TIMESTAMP,
    `UPDATETEMPTIME`      TIMESTAMP,
    PRIMARY KEY (ID) NOT ENFORCED
) WITH (
      'connector' = 'mysql-cdc',
      ${dbserver1},
      'database-name' = 'db1',
      'table-name' = 'COINSDETAIL',
      'server-time-zone' = 'Asia/Shanghai',
      'scan.startup.mode' = 'initial',
      'scan.snapshot.fetch.size' = '10240',
      'scan.incremental.close-idle-reader.enabled' = 'true'
      );

CREATE TABLE `COINSDETAIL_TEMP`
(
    `DETAILNO`            STRING,
    `SERIALNO`            INT,
    `COINSCODE`           STRING,
    `COINSNAME`           STRING,
    `CURRENCY`            STRING,
    `COINSAMOUNT`         DOUBLE,
    `OPERATEFEE`          DOUBLE,
    `FLAG`                STRING,
    `COINSTYPE`           STRING,
    `INSTANCEID`          STRING,
    `FORMCREATEDATE`      TIMESTAMP,
    `FORMUPDATEDATE`      TIMESTAMP,
    `DEFINEID`            STRING,
    `FORMCREATEBYID`      STRING,
    `FORMUPDATEBYID`      STRING,
    `INSERTTEMPTIME`      TIMESTAMP,
    `UPDATETEMPTIME`      TIMESTAMP,
    PRIMARY KEY (`DETAILNO`, `SERIALNO`) NOT ENFORCED
) WITH (
      'connector' = 'jdbc',
      ${testoracledb},
      'table-name' = 'COINSDETAIL_TEMP',
      'sink.buffer-flush.max-rows' = '20480',
      'scan.auto-commit' = 'false',
      'scan.fetch-size' = '10240'
      );

INSERT INTO COINSDETAIL_TEMP
SELECT DETAILNO,
       SERIALNO,
       COINSCODE,
       COINSNAME,
       CURRENCY,
       COINSAMOUNT,
       OPERATEFEE,
       FLAG,
       COINSTYPE,
       INSTANCEID,
       FORMCREATEDATE,
       FORMUPDATEDATE,
       DEFINEID,
       FORMCREATEBYID,
       FORMUPDATEBYID,
       INSERTTEMPTIME,
       UPDATETEMPTIME
FROM COINSDETAIL;