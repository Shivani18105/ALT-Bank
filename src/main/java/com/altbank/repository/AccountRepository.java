package com.altbank.repository;
import com.altbank.entity.Account;
import com.altbank.entity.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :number")
    Optional<Account> findByAccountNumberForUpdate(@Param("number") String number);
    long countByStatus(AccountStatus status);
}
