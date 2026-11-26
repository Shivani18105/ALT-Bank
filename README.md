# ALT Bank — Multi-Threaded Banking Transaction Processing System

A modular-monolith Spring Boot 3 / Java 17 / PostgreSQL 16 transaction engine for **ALT Bank**: deposits, withdrawals and
transfers from ATM, Internet, Mobile and Branch channels, processed concurrently on a **bounded worker pool**, with
**pessimistic row locking**, **database-enforced idempotency**, an **immutable audit trail**, structured JSON logging,
JWT security with role-based access, Prometheus metrics, a 50,000-transaction benchmark tool and a small web UI.

> **Honest status.** This repository was produced in an environment **without Maven, PostgreSQL, Docker or network access**.
> The code was syntax-checked, but **the build, the tests, the benchmark and the EXPLAIN analysis have not been executed by the
> author of this scaffold.** No benchmark numbers or query plans are claimed anywhere; `docs/performance/` and
> `docs/database/` contain *pending* templates that the tooling here fills with real measurements when you run it.
> The only code actually executed was a dependency-free smoke check (`scripts/standalone-check`: money rules, state machine, bounded pool, rate limiter, masker, 50,000-request workload generator — 19 checks passed). Section "Verify it yourself" below is the exact checklist. Please run it before submitting.

> **New here? Follow [`RUN_GUIDE.md`](RUN_GUIDE.md)** — the complete step-by-step setup (secrets, DB connection, Docker/no-Docker, demo, tests, benchmark, troubleshooting).

## Contents
1. [Features](#features) · 2. [Architecture](#architecture) · 3. [Technology](#technology-stack) · 4. [Quick start](#quick-start)
5. [Demo credentials](#demo-credentials-dev-profile-only) · 6. [Concurrency, locking, idempotency](#concurrency-locking-and-idempotency)
7. [API](#api) · 8. [Tests](#running-tests) · 9. [Performance test](#running-the-50000-transaction-performance-test)
10. [SQL reports & EXPLAIN](#sql-reports-and-explain-analyze) · 11. [Security](#security) · 12. [Mandatory vs additional](#mandatory-assignment-requirements-vs-additions)
13. [Limitations](#known-limitations) · 14. [Future work](#future-improvements)

## Features
- Deposit, withdrawal, account-to-account transfer; customers, accounts, balances (`NUMERIC(19,2)` / `BigDecimal`), account status, history.
- Outcomes **SUCCESS / FAILED / REJECTED**, each with a stored reason, category, thread, duration and correlation id.
- Idempotency by `transactionId`: application check **and** `UNIQUE` constraint; race-safe (`INSERT … ON CONFLICT DO NOTHING`).
- Deterministic lock order (ascending account id) for transfers; guarded `UPDATE … WHERE balance >= ?`; `CHECK (balance >= 0)`.
- Append-only audit (`transaction_audit` trigger blocks UPDATE/DELETE) + `failed_transaction` investigation table.
- Five required SQL reports, EXPLAIN script, workload generator and 1/5/10/20-worker benchmark with invariant verification.

## Architecture
```
Client/UI ─► Security (JWT, rate limit, headers) ─► Controller ─► Service (authz, idempotency-key)
          ─► TransactionExecutionService ─► BoundedWorkerPool (N threads, bounded queue)
          ─► TransactionProcessor (1 DB tx per request) ─► JDBC repositories ─► PostgreSQL (HikariCP)
Cross-cutting: SLF4J/Logback JSON logs + MDC, Micrometer metrics, global exception handler, audit tables
```
Diagrams: [`docs/architecture/architecture.md`](docs/architecture/architecture.md), [`docs/er-diagram/er-diagram.md`](docs/er-diagram/er-diagram.md).
Design note (transaction boundaries, locking, idempotency, indexes): [`docs/design/design-note.md`](docs/design/design-note.md).
Security: [`docs/security/security.md`](docs/security/security.md). Requirement traceability: [`docs/requirements-traceability.md`](docs/requirements-traceability.md).

## Technology stack
| Area | Choice | Why |
|---|---|---|
| Language/build | Java 17+, Maven | assignment-mandatory |
| Framework | Spring Boot 3.3 (web, security, validation, actuator) | mature, testable |
| Data access | **Spring JDBC (`JdbcTemplate`)**, not JPA | assignment allows JDBC; explicit `FOR UPDATE`, guarded `UPDATE`, `ON CONFLICT` are first-class and visible |
| DB | PostgreSQL 16 + Flyway | row locks, partial/covering indexes, `ON CONFLICT`, EXPLAIN; versioned migrations |
| Logging | SLF4J + Logback + logstash-logback-encoder | JSON events, MDC fields |
| Security | Spring Security, JWT (jjwt), BCrypt | stateless API |
| Observability | Micrometer + Prometheus, Actuator | |
| API docs | springdoc OpenAPI (dev profile) | |
| Tests | JUnit 5, Mockito (via starter), Testcontainers (PostgreSQL) | real DB for concurrency |
| UI | Dependency-free static HTML/CSS/JS served by Spring Boot | no build step, single origin, strict CSP |

## Quick start
Prerequisites: Docker + Docker Compose (for the stack), or JDK 17+, Maven 3.9+ and a local PostgreSQL.

```bash
cp .env.example .env            # then edit: DATABASE_PASSWORD and ALT_JWT_SECRET (openssl rand -base64 48)
docker compose up --build       # postgres + API + UI, profile "dev" (demo data)
```
- UI: http://localhost:8080/  ·  Swagger UI (dev): http://localhost:8080/swagger-ui.html  ·  health: `/actuator/health`
- Demo flow in a terminal: `scripts/demo.sh` (needs `curl`, `jq`).

Without Docker:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/altbank DATABASE_USERNAME=altbank DATABASE_PASSWORD=... ALT_JWT_SECRET=...
export SPRING_PROFILES_ACTIVE=dev
mvn -f backend/pom.xml spring-boot:run
```
Flyway runs on startup (`database/migrations`, plus `database/seed` in the dev profile only).

### Environment variables
| Variable | Meaning |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | JDBC connection (required) |
| `ALT_JWT_SECRET` | HMAC secret, **≥ 32 bytes**, required; the app refuses to start otherwise |
| `SPRING_PROFILES_ACTIVE` | `dev` (seed + Swagger), `prod`, `test`, `perf` |
| `ALT_WORKER_POOL_SIZE` (10) | API worker threads; keep ≤ `HIKARI_MAX_POOL_SIZE` |
| `HIKARI_MAX_POOL_SIZE` (20) | DB connections |
| `ALT_CORS_ALLOWED_ORIGINS` | comma-separated origins; empty = same-origin only |

### Database migration
Flyway versions: `V1` customer/user · `V2` account · `V3` transaction · `V4` audit + failed · `V5` indexes · `V100` demo seed (dev only).
Reporting SQL lives in `database/reports/` and is the single source used by both the API and psql.

## Demo credentials (dev profile only)
Seeded by `database/seed/V100__demo_seed.sql`; synthetic data; **never use these outside local development** (the `prod` profile does not load them).

| Username | Password | Role | Notes |
|---|---|---|---|
| `admin` | `Admin@AltBank1` | ADMIN | everything, reports, account admin |
| `employee` | `Employee@AltBank1` | BANK_EMPLOYEE | any account, create customers/accounts |
| `auditor` | `Auditor@AltBank1` | AUDITOR | read-only transactions, audit trail, reports |
| `alice` | `Alice@AltBank1` | CUSTOMER | `ALT1000000001` (₹10,000 – concurrency demo), `ALT1000000002` (₹50,000) |
| `bob` | `Bob@AltBank1` | CUSTOMER | `ALT1000000003` (₹25,000), `ALT1000000004` (**INACTIVE**, ₹5,000) |
| `carol` | `Carol@AltBank1` | CUSTOMER | `ALT1000000005` (₹1,00,000), `ALT1000000006` (₹2,500) |

## Concurrency, locking and idempotency
- **One database transaction per request** (`TransactionProcessor`): claim ID → validate → lock → apply → final status + audit → commit. `SUCCESS` is returned only after commit.
- **Pessimistic locking**: `SELECT … FOR UPDATE` on the account row(s); the balance check and the `UPDATE` happen while the lock is held, so no lost update and no overdraft. Transfers lock **lower account id first**, then higher → no circular wait.
- **Defence in depth**: guarded `UPDATE … WHERE balance >= ?` (must affect exactly 1 row) and `CHECK (balance >= 0)`.
- **Idempotency**: `INSERT … ON CONFLICT (transaction_id) DO NOTHING` is the gate. Two simultaneous duplicates: the second blocks on the unique index until the first commits, then gets no row back and returns the stored outcome (header `Idempotent-Replay: true`). Same ID with a different payload/requester → `409 DUPLICATE_TRANSACTION`.
- **Bounded concurrency**: `BoundedWorkerPool` (fixed threads + bounded queue; API rejects with 503 when full). Pool size ≤ Hikari size because every in-flight transaction holds one connection.
- **Retries**: deadlock/lock-timeout victims are retried at most 3 times; any other error rolls back and is recorded as `FAILED / PROCESSING_ERROR` in a fresh small transaction.
- Details and trade-offs: [`docs/design/design-note.md`](docs/design/design-note.md).

### Status taxonomy
| Status | Meaning | Examples |
|---|---|---|
| `SUCCESS` | committed, money moved | – |
| `REJECTED` | request invalid/ineligible; no money moved | invalid/zero/negative/over-precise amount, unknown account, inactive account, same-account transfer, currency mismatch |
| `FAILED` | valid request that could not be executed | `INSUFFICIENT_BALANCE` (decided under the row lock), `PROCESSING_ERROR` (technical, rolled back) |

State machine: `RECEIVED → PROCESSING → SUCCESS | FAILED | REJECTED`, `RECEIVED → REJECTED | FAILED`; terminal states never change.
Unauthorized attempts (403) are logged but **not persisted** so an attacker cannot "burn" someone else's future transaction IDs.

## API
Base `/api/v1`. Full schema in Swagger UI (dev). `Authorization: Bearer <token>` everywhere except login.

| Method & path | Roles | Purpose |
|---|---|---|
| `POST /auth/login` | public (rate-limited) | username/password → JWT |
| `POST /customers`, `POST /accounts`, `PATCH /accounts/{no}/status` | BANK_EMPLOYEE, ADMIN | onboarding / status |
| `GET /accounts/mine`, `GET /accounts/{no}`, `/balance`, `/transactions?page&size` | owner; staff/auditor any | accounts & history |
| `POST /transactions/deposit | withdraw | transfer` | CUSTOMER (own source), BANK_EMPLOYEE, ADMIN | money movement |
| `GET /transactions/{id}` | involved customer; staff/auditor | status |
| `GET /transactions/{id}/audit` | AUDITOR, ADMIN | immutable status trail |
| `GET /reports/{summary,top-accounts,failed-transactions,daily-volume,busy-accounts,recent-failures}`, `/reports/customers/{no}/history` | ADMIN, AUDITOR | the 5 required reports + dashboard |

**`transactionId` is the idempotency key.** The optional `Idempotency-Key` header is an alias: if only one is sent it is used; if both are sent they must match (else 400). Example:
```bash
TOKEN=$(curl -s localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"username":"alice","password":"Alice@AltBank1"}' | jq -r .accessToken)
curl -i localhost:8080/api/v1/transactions/transfer -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"transactionId":"TXN100234","sourceAccount":"ALT1000000001","destinationAccount":"ALT1000000003","amount":2500.00,"channel":"MOBILE_BANKING"}'
```
Errors use one shape: `{timestamp,status,code,message,path,transactionId?,transactionStatus?,correlationId}`. Send/receive `X-Correlation-Id`.

## Running tests
```bash
mvn -f backend/pom.xml test        # unit tests (no Docker)
mvn -f backend/pom.xml verify      # + integration & concurrency tests (needs Docker for Testcontainers)
```
| Test | What it proves |
|---|---|
| `ConcurrencyIT.mandatoryScenario_*` (25 repetitions + via worker pool) | ₹10,000 with concurrent ₹7,000 / ₹5,000 withdrawals: exactly one succeeds, final 3,000 or 5,000, never negative |
| `manyConcurrentWithdrawals_neverOverdraw` | 200×₹100 vs ₹10,000 → exactly 100 succeed, balance 0 |
| `duplicateTransactionIds_haveFinancialImpactExactlyOnce` | 20 concurrent identical IDs → one effect, one row, 19 replays |
| `concurrentTransfersOnSameAccount_conserveTotal`, `oppositeDirectionTransfers_doNotDeadlock`, `mixedDepositsAndWithdrawals_reconcile` | conservation, no deadlocks, reconciliation |
| `RollbackIT` | crash between debit and credit → debit rolled back, `FAILED` recorded |
| `ApiSecurityIT` | 401/403/404 rules, IDOR, lockout, replay header, error model, mass-assignment, headers |
| unit tests | state machine, money rules, validator, JWT, rate limiter, masking, worker pool, workload generator |

## Running the 50,000-transaction performance test
Uses the separate database `altbank_perf` (created by `database/docker-init`; the run **truncates transaction tables** there).
```bash
docker compose up -d postgres
mvn -f backend/pom.xml -DskipTests package
scripts/run-perf.sh                    # 50,000 requests × workers 1,5,10,20 (override: PERF_TRANSACTIONS, PERF_WORKERS)
```
Writes `performance/results/latest.md` and `perf-<timestamp>.json`: time, success/rejected/failed, replays, TPS, speed-up, avg/P95 latency, Hikari usage, PostgreSQL wait-event samples, commits/rollbacks/deadlocks, CPU, and **post-run invariant checks** (no negative balance, balances reconcile, one row per ID, every row audited). Copy the real table into `docs/performance/performance-report.md`. Start with the same `HIKARI_MAX_POOL_SIZE=10` and `PERF_WORKERS=20` to *see* connection-pool saturation.

## SQL reports and EXPLAIN ANALYZE
`database/reports/report1…5_*.sql` (+ `summary.sql`, `recent_failures.sql`, `safe_monetary_updates.sql`). Run `scripts/run-explain.sh` after a perf run to execute `database/explain/explain_queries.sql` (six plans, including an index-on/off comparison) and fill [`docs/database/explain-analysis.md`](docs/database/explain-analysis.md).

## Security
JWT (HS256, 30 min), BCrypt(12), lockout after 5 failed logins (15 min), per-IP login and per-user transaction rate limits, object-level authorization (IDOR), strict JSON (unknown fields rejected), CSP/X-Frame-Options/nosniff/no-store headers, closed CORS by default, parameterised SQL only, masked account numbers in logs, no stack traces to clients, secrets only from environment, Actuator restricted. Full list, threat model and *what is not claimed*: [`docs/security/security.md`](docs/security/security.md).

## Mandatory assignment requirements vs additions
**Mandatory (from the PDFs):** Java 17+/Maven; PostgreSQL; JDBC; bounded thread pool; deposit/withdraw/transfer; validation gate; unique transaction ID + idempotency (app + DB); SUCCESS/FAILED/REJECTED with reasons; DB transactions; pessimistic locking with deterministic order; SLF4J+Logback structured logs; custom exceptions; normalized DDL (PK/FK/CHECK/UNIQUE) + justified indexes; 5 reports; ≥3 EXPLAIN analyses; 50,000-transaction run with 1/5/10/20 workers; unit + concurrency integration tests; sample logs; architecture + ER diagrams; README; design note.
**Additional:** JWT/RBAC, rate limiting, lockout, Flyway, Testcontainers, Docker, Micrometer/Prometheus, OpenAPI, web UI, immutable-audit trigger, invariant checker in the benchmark.
Checklist with status: [`docs/requirements-traceability.md`](docs/requirements-traceability.md).

## Known limitations
- Not executed by the scaffold's author (see top); expect to fix small compile/config issues on first build.
- Rate limiting is per node (in memory); JWTs are not revocable before expiry (30 min); no refresh tokens/MFA.
- Single currency (INR); no fees, limits per day, or ledger (double-entry) tables — balances are the source of truth with audit.
- Account numbers in API responses are shown in full to authorized users; only logs are masked.
- Sample logs in `docs/sample-logs` are an illustrative specimen of the format, not captured output.

## Future improvements
Double-entry ledger, outbox/events, read replicas for reports, partitioning `bank_transaction` by month, refresh tokens + MFA, distributed rate limiting, HSM/KMS-managed keys, per-account daily limits, `SKIP LOCKED` queue ingestion.

## Verify it yourself (before you submit)
1. `mvn -f backend/pom.xml verify` — all unit + integration tests green (needs Docker). Fix anything that fails and rerun.
2. `docker compose up --build`, log in as `alice`, run `scripts/demo.sh`.
3. `scripts/run-perf.sh` — commit `performance/results/latest.md`; paste into the performance report with your own bottleneck analysis.
4. `scripts/run-explain.sh` — paste plans/observations into `docs/database/explain-analysis.md`.
5. Capture real logs from `logs/alt-bank.json` (dev) into `docs/sample-logs/`.
