package com.altbank.repository;
import com.altbank.entity.TransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> { }
