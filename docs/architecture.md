# Architecture

```text
Client / Simulator
       |
       v
REST Controller / Batch Input
       |
       v
TransactionService
  | validation
  | idempotency
  | transaction boundary
  | deterministic locking
       |
       v
Spring Data JPA
       |
       v
PostgreSQL
  CUSTOMER
  ACCOUNT
  TRANSACTION
  TRANSACTION_AUDIT
  FAILED_TRANSACTION

Cross-cutting: SLF4J logging, exception mapping, Flyway migrations, Actuator
```
