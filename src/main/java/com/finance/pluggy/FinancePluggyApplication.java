package com.finance.pluggy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class FinancePluggyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancePluggyApplication.class, args);
    }
}
