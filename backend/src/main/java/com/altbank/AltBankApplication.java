package com.altbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** ALT Bank - Multi-Threaded Banking Transaction Processing System. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AltBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(AltBankApplication.class, args);
    }
}
