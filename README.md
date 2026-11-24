# ALT Bank — Multi-Threaded Banking Transaction Processing System

A Java 17+ banking transaction engine designed around transactional consistency, idempotency, auditability, concurrency safety, SQL proficiency, and measurable performance.

## Assignment coverage
- Deposit, withdrawal, transfer
- Active-account and amount validation
- Insufficient-balance protection
- Unique transaction IDs and idempotent duplicate handling
- PostgreSQL transactions with pessimistic row locking
- Deterministic account locking order for transfers
- Structured SLF4J logging and persisted audit records
- Failure records with reasons
- Normalized DDL, constraints, and workload-oriented indexes
- REST API plus Actuator health endpoint
- Testcontainers/JUnit foundation for concurrency and integration testing
- Planned 50,000+ transaction benchmark at 1/5/10/20 workers

The assignment requires Java 17+, PostgreSQL/MySQL, JDBC or Spring Data JPA, SLF4J + Logback/Log4j2, JUnit 5, 50,000+ transactions, and evidence for concurrency, duplicate processing, SQL analysis, logging, tests, and performance. See the supplied assignment PDFs for the authoritative wording.

## Run
1. Copy `.env.example` to `.env` and set a local password.
2. Start PostgreSQL: `docker compose up -d postgres`
3. Run: `./mvnw spring-boot:run` (or `mvn spring-boot:run` when Maven is installed).
4. POST transactions to `/api/v1/transactions`.

Example:
```json
{
  "transactionId": "TXN100234",
  "type": "TRANSFER",
  "amount": 1250.00,
  "sourceAccount": "ALT1000000001",
  "destinationAccount": "ALT1000000002"
}
```

## Important
Do not commit `.env`, credentials, API keys, tokens, or real customer data.

## Git history
The repository is intentionally organized into meaningful milestones. Each milestone should be tested before the next commit is made.

## Performance evidence
The final submission must contain real measurements for 50,000+ transactions using 1, 5, 10 and 20 workers. Results belong in `performance/results.csv`; leave values blank until actually measured.
