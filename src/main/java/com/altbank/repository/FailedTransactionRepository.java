package com.altbank.repository;
import com.altbank.entity.FailedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FailedTransactionRepository extends JpaRepository<FailedTransaction, Long> { }
