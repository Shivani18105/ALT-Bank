package com.altbank.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "failed_transaction")
public class FailedTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", nullable = false, length = 64) private String transactionId;
    @Column(name = "error_category", nullable = false, length = 80) private String errorCategory;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false) private Instant createdAt = Instant.now();

    public void setTransactionId(String v) { transactionId = v; }
    public void setErrorCategory(String v) { errorCategory = v; }
    public void setReason(String v) { reason = v; }
}
