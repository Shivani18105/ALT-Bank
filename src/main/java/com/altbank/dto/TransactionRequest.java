package com.altbank.dto;

import com.altbank.entity.TransactionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank @Size(max=64) String transactionId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value="0.01") BigDecimal amount,
        @Size(max=24) String sourceAccount,
        @Size(max=24) String destinationAccount) {}
