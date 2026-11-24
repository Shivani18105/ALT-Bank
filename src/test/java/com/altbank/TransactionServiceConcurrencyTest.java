package com.altbank;

import com.altbank.dto.TransactionRequest;
import com.altbank.entity.TransactionType;
import com.altbank.repository.AccountRepository;
import com.altbank.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class TransactionServiceConcurrencyTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired TransactionService service;
    @Autowired AccountRepository accounts;

    @Test
    void concurrentWithdrawalsMustNotBothSucceed() throws Exception {
        var account = accounts.findByAccountNumber("ALT1000000001").orElseThrow();
        account.setBalance(new BigDecimal("10000.00"));
        accounts.saveAndFlush(account);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> service.process(new TransactionRequest("CONC-A", TransactionType.WITHDRAWAL,
                    new BigDecimal("7000.00"), "ALT1000000001", null)));
            var b = pool.submit(() -> service.process(new TransactionRequest("CONC-B", TransactionType.WITHDRAWAL,
                    new BigDecimal("5000.00"), "ALT1000000001", null)));
            var ra = a.get(10, TimeUnit.SECONDS); var rb = b.get(10, TimeUnit.SECONDS);
            long successes = java.util.stream.Stream.of(ra, rb).filter(r -> r.status().name().equals("SUCCESS")).count();
            assertEquals(1, successes);
            assertTrue(accounts.findByAccountNumber("ALT1000000001").orElseThrow().getBalance().signum() >= 0);
        } finally { pool.shutdownNow(); }
    }
}
