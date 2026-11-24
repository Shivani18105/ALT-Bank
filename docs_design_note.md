# Design Note

## Transaction boundary
Each money movement is processed inside one database transaction. A success response is produced only after the transaction can complete successfully.

## Locking
The implementation uses PostgreSQL/JPA pessimistic write locking. Transfers lock the lower account identifier first and the higher identifier second, reducing avoidable deadlock cycles.

## Idempotency
The application checks transactionId before processing and the database enforces a UNIQUE constraint. The database constraint is the final guard against duplicate financial impact.

## Money
Amounts use `BigDecimal`/PostgreSQL `NUMERIC(19,2)` rather than floating point.

## Audit versus logging
Business audit records are persisted in `transaction_audit` and failures in `failed_transaction`. Logs describe execution and never intentionally print credentials or full sensitive account data.
