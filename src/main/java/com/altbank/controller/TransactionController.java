package com.altbank.controller;

import com.altbank.dto.TransactionRequest;
import com.altbank.dto.TransactionResponse;
import com.altbank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<TransactionResponse> process(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.process(request));
    }
}
