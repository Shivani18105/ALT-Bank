CREATE TABLE customer (
    customer_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE account (
    account_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(customer_id),
    account_number VARCHAR(24) NOT NULL UNIQUE,
    balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE','BLOCKED')),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAWAL','TRANSFER')),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    source_account VARCHAR(24),
    destination_account VARCHAR(24),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RECEIVED','PROCESSING','SUCCESS','FAILED','REJECTED')),
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE transaction_audit (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL REFERENCES transaction(transaction_id),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    thread_name VARCHAR(120) NOT NULL,
    operation VARCHAR(30) NOT NULL,
    duration_ms BIGINT NOT NULL,
    result VARCHAR(20) NOT NULL,
    reason VARCHAR(500)
);
CREATE TABLE failed_transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL REFERENCES transaction(transaction_id),
    error_category VARCHAR(80) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_account_customer ON account(customer_id);
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_type_created ON transaction(type, created_at);
CREATE INDEX idx_audit_transaction ON transaction_audit(transaction_id);
CREATE INDEX idx_failed_reason ON failed_transaction(error_category);
