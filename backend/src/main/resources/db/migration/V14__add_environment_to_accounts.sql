ALTER TABLE accounts ADD COLUMN environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX';
CREATE INDEX idx_accounts_environment ON accounts(environment);
