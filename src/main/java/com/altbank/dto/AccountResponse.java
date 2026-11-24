package com.altbank.dto;
import com.altbank.entity.AccountStatus;
import java.math.BigDecimal;
public record AccountResponse(String accountNumber, BigDecimal balance, String currency, AccountStatus status) {}
