package com.altbank.service;

import com.altbank.dto.TransactionRequest;
import com.altbank.dto.TransactionResponse;
import com.altbank.entity.*;
import com.altbank.exception.*;
import com.altbank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final AccountRepository accounts;
    private final BankTransactionRepository transactions;
    private final TransactionAuditRepository audits;
    private final FailedTransactionRepository failures;

    public TransactionService(AccountRepository accounts, BankTransactionRepository transactions,
                              TransactionAuditRepository audits, FailedTransactionRepository failures) {
        this.accounts = accounts; this.transactions = transactions; this.audits = audits; this.failures = failures;
    }

    public TransactionResponse process(TransactionRequest request) {
        var existing = transactions.findByTransactionId(request.transactionId());
        if (existing.isPresent()) {
            log.info("transactionId={} result=DUPLICATE", request.transactionId());
            return toResponse(existing.get());
        }
        try {
            return processNew(request);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent request may have won the unique transaction_id race.
            return transactions.findByTransactionId(request.transactionId())
                    .map(this::toResponse)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    protected TransactionResponse processNew(TransactionRequest request) {
        Instant started = Instant.now();
        log.info("transactionId={} thread={} operation={} result=RECEIVED", request.transactionId(),
                Thread.currentThread().getName(), request.type());

        BankTransaction tx = new BankTransaction();
        tx.setTransactionId(request.transactionId()); tx.setType(request.type()); tx.setAmount(request.amount());
        tx.setSourceAccount(request.sourceAccount()); tx.setDestinationAccount(request.destinationAccount());
        tx.setStatus(TransactionStatus.RECEIVED);
        transactions.saveAndFlush(tx);
        tx.setStatus(TransactionStatus.PROCESSING);
        try {
            validate(request);
            switch (request.type()) {
                case DEPOSIT -> deposit(request);
                case WITHDRAWAL -> withdraw(request);
                case TRANSFER -> transfer(request);
            }
            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setReason("Processed successfully");
            transactions.save(tx);
            audit(request, "SUCCESS", started, null);
            log.info("transactionId={} thread={} operation={} result=SUCCESS durationMs={}", request.transactionId(),
                    Thread.currentThread().getName(), request.type(), Duration.between(started, Instant.now()).toMillis());
            return toResponse(tx);
        } catch (RuntimeException ex) {
            tx.setStatus(ex instanceof InvalidTransactionException || ex instanceof AccountNotFoundException ||
                    ex instanceof InsufficientBalanceException ? TransactionStatus.REJECTED : TransactionStatus.FAILED);
            tx.setReason(ex.getMessage());
            transactions.save(tx);
            FailedTransaction failure = new FailedTransaction(); failure.setTransactionId(request.transactionId());
            failure.setErrorCategory(ex.getClass().getSimpleName()); failure.setReason(ex.getMessage()); failures.save(failure);
            audit(request, tx.getStatus().name(), started, ex.getMessage());
            log.warn("transactionId={} thread={} operation={} result={} reason={} durationMs={}", request.transactionId(),
                    Thread.currentThread().getName(), request.type(), tx.getStatus(), ex.getMessage(),
                    Duration.between(started, Instant.now()).toMillis());
            return toResponse(tx);
        }
    }

    private void validate(TransactionRequest r) {
        if (r.amount() == null || r.amount().signum() <= 0) throw new InvalidTransactionException("Amount must be positive");
        if (r.type() == TransactionType.DEPOSIT && blank(r.destinationAccount())) throw new InvalidTransactionException("Destination account is required");
        if (r.type() == TransactionType.WITHDRAWAL && blank(r.sourceAccount())) throw new InvalidTransactionException("Source account is required");
        if (r.type() == TransactionType.TRANSFER && (blank(r.sourceAccount()) || blank(r.destinationAccount()) || r.sourceAccount().equals(r.destinationAccount())))
            throw new InvalidTransactionException("Transfer requires two different accounts");
    }
    private boolean blank(String s){ return s == null || s.isBlank(); }

    private void deposit(TransactionRequest r) {
        Account a = lockActive(r.destinationAccount());
        a.setBalance(a.getBalance().add(r.amount()));
    }
    private void withdraw(TransactionRequest r) {
        Account a = lockActive(r.sourceAccount());
        if (a.getBalance().compareTo(r.amount()) < 0) throw new InsufficientBalanceException("Insufficient balance");
        a.setBalance(a.getBalance().subtract(r.amount()));
    }
    private void transfer(TransactionRequest r) {
        String first = r.sourceAccount().compareTo(r.destinationAccount()) < 0 ? r.sourceAccount() : r.destinationAccount();
        String second = first.equals(r.sourceAccount()) ? r.destinationAccount() : r.sourceAccount();
        Account a = lockActive(first); Account b = lockActive(second);
        Account source = a.getAccountNumber().equals(r.sourceAccount()) ? a : b;
        Account destination = source == a ? b : a;
        if (source.getBalance().compareTo(r.amount()) < 0) throw new InsufficientBalanceException("Insufficient balance");
        source.setBalance(source.getBalance().subtract(r.amount()));
        destination.setBalance(destination.getBalance().add(r.amount()));
    }
    private Account lockActive(String number) {
        Account a = accounts.findByAccountNumberForUpdate(number).orElseThrow(() -> new AccountNotFoundException("Account not found: " + number));
        if (a.getStatus() != AccountStatus.ACTIVE) throw new InvalidTransactionException("Account is not active: " + number);
        return a;
    }
    private void audit(TransactionRequest r, String result, Instant started, String reason) {
        TransactionAudit a = new TransactionAudit(); a.setTransactionId(r.transactionId());
        a.setThreadName(Thread.currentThread().getName()); a.setOperation(r.type().name()); a.setResult(result);
        a.setDurationMs(Duration.between(started, Instant.now()).toMillis()); a.setReason(reason); audits.save(a);
    }
    private TransactionResponse toResponse(BankTransaction tx) { return new TransactionResponse(tx.getTransactionId(), tx.getStatus(), tx.getAmount(), tx.getReason()); }
}
