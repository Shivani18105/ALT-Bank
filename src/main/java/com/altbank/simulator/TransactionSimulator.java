package com.altbank.simulator;

import com.altbank.dto.TransactionRequest;
import com.altbank.entity.TransactionType;
import com.altbank.service.TransactionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class TransactionSimulator {
    private final TransactionService service;
    public TransactionSimulator(TransactionService service) { this.service = service; }

    public SimulationResult run(List<TransactionRequest> requests, int workers) throws InterruptedException {
        long start = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (TransactionRequest request : requests) futures.add(executor.submit(() -> service.process(request)));
            int successful = 0, failed = 0;
            for (Future<?> f : futures) {
                try {
                    var response = (com.altbank.dto.TransactionResponse) f.get();
                    if (response.status().name().equals("SUCCESS")) successful++; else failed++;
                } catch (ExecutionException e) { failed++; }
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            double tps = elapsedMs == 0 ? successful + failed : ((successful + failed) * 1000.0) / elapsedMs;
            return new SimulationResult(workers, elapsedMs, successful, failed, tps);
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public record SimulationResult(int workers, long totalTimeMs, int successful, int failed, double transactionsPerSecond) {}
}
