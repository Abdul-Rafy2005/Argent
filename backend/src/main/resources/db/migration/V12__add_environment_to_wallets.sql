ALTER TABLE wallets ADD COLUMN environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX';
CREATE INDEX idx_wallets_environment ON wallets(environment);
