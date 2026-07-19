ALTER TABLE ledger_entries ADD COLUMN environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX';
CREATE INDEX idx_ledger_entries_environment ON ledger_entries(environment);
