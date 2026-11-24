package com.altbank.dto;
import com.altbank.entity.TransactionStatus;
import java.math.BigDecimal;
public record TransactionResponse(String transactionId, TransactionStatus status, BigDecimal amount, String reason) {}
