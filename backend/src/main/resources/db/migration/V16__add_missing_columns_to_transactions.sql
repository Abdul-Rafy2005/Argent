-- Add missing columns to transactions table for Phase 5

-- Add environment column (matches wallet/account/ledger_entries pattern)
ALTER TABLE transactions ADD COLUMN environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX';

-- Add failure_reason for failed transactions
ALTER TABLE transactions ADD COLUMN failure_reason TEXT;

-- Add version for optimistic locking
ALTER TABLE transactions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add updated_at with default
ALTER TABLE transactions ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add index for environment filtering
CREATE INDEX idx_transactions_environment ON transactions(environment);
