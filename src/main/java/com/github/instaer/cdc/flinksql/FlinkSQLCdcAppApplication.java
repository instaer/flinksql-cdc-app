package com.github.instaer.cdc.flinksql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlinkSQLCdcAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlinkSQLCdcAppApplication.class, args);
    }
}