package com.altbank.controller;

import com.altbank.dto.AccountResponse;
import com.altbank.repository.AccountRepository;
import com.altbank.exception.AccountNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountRepository accounts;
    public AccountController(AccountRepository accounts) { this.accounts = accounts; }
    @GetMapping("/{accountNumber}")
    public AccountResponse get(@PathVariable String accountNumber) {
        var a = accounts.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return new AccountResponse(a.getAccountNumber(), a.getBalance(), a.getCurrency(), a.getStatus());
    }
}
