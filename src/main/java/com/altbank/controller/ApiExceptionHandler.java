package com.altbank.controller;

import com.altbank.exception.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({InvalidTransactionException.class, InsufficientBalanceException.class, AccountNotFoundException.class})
    ResponseEntity<Map<String,Object>> rejected(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("timestamp", Instant.now(), "error", ex.getClass().getSimpleName(), "message", ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> internal(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("timestamp", Instant.now(), "error", "InternalServerError", "message", "Unexpected error"));
    }
}
