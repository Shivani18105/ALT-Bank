package com.altbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AltBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(AltBankApplication.class, args);
    }
}
