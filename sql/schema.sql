-- ============================================================
-- Bank POC - PostgreSQL Schema
-- Database: bankpoc
-- ============================================================

-- Drop tables if they exist (for clean re-creation)
DROP TABLE IF EXISTS file_registry CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS batch_parts CASCADE;
DROP TABLE IF EXISTS batches CASCADE;

-- ============================================================
-- 1. batches - Tracks bulk payment batch lifecycle
-- ============================================================
CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_transactions INT DEFAULT 0,
    successful_transactions INT DEFAULT 0,
    failed_transactions INT DEFAULT 0,
    total_amount DECIMAL(18,2) DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- ============================================================
-- 2. batch_parts - Tracks individual file parts of a batch
-- ============================================================
CREATE TABLE batch_parts (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(batch_id),
    part_number INT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    record_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(batch_id, part_number)
);

-- ============================================================
-- 3. transactions - Individual payment records
-- ============================================================
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(batch_id),
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    sender_account VARCHAR(20) NOT NULL,
    sender_ifsc VARCHAR(11) NOT NULL,
    sender_name VARCHAR(100),
    receiver_account VARCHAR(20) NOT NULL,
    receiver_ifsc VARCHAR(11) NOT NULL,
    receiver_name VARCHAR(100),
    amount DECIMAL(18,2) NOT NULL,
    payment_mode VARCHAR(20) NOT NULL DEFAULT 'NEFT',
    narration VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    npci_reference VARCHAR(50),
    npci_response_code VARCHAR(10),
    failure_category VARCHAR(30),
    failure_reason TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- ============================================================
-- 4. audit_log - Tracks all state changes for traceability
-- ============================================================
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(50),
    entity_type VARCHAR(30) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    details TEXT,
    source_component VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) DEFAULT 'SYSTEM'
);

-- ============================================================
-- 5. file_registry - Duplicate file detection
-- ============================================================
CREATE TABLE file_registry (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_checksum VARCHAR(128) NOT NULL,
    file_size_bytes BIGINT,
    batch_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'REGISTERED',
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_checksum)
);

-- ============================================================
-- Indexes for query performance
-- ============================================================
CREATE INDEX idx_batches_status ON batches(status);
CREATE INDEX idx_batches_created_at ON batches(created_at);
CREATE INDEX idx_transactions_batch_id ON transactions(batch_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_failure_category ON transactions(failure_category);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_idempotency ON transactions(idempotency_key);
CREATE INDEX idx_audit_log_entity_id ON audit_log(entity_id);
CREATE INDEX idx_audit_log_entity_type ON audit_log(entity_type);
CREATE INDEX idx_file_registry_checksum ON file_registry(file_checksum);
CREATE INDEX idx_file_registry_batch ON file_registry(batch_id);

-- ============================================================
-- Trigger for auto-updating updated_at columns
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_batches_updated_at
    BEFORE UPDATE ON batches
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
