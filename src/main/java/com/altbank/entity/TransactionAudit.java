package com.altbank.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transaction_audit")
public class TransactionAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", nullable = false, length = 64) private String transactionId;
    @Column(nullable = false) private Instant timestamp = Instant.now();
    @Column(nullable = false, length = 120) private String threadName;
    @Column(nullable = false, length = 30) private String operation;
    @Column(nullable = false) private long durationMs;
    @Column(nullable = false, length = 20) private String result;
    @Column(length = 500) private String reason;

    public void setTransactionId(String v) { transactionId = v; }
    public void setThreadName(String v) { threadName = v; }
    public void setOperation(String v) { operation = v; }
    public void setDurationMs(long v) { durationMs = v; }
    public void setResult(String v) { result = v; }
    public void setReason(String v) { reason = v; }
}
