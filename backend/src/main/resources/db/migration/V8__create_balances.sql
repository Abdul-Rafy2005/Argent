CREATE TABLE balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(id),
    current NUMERIC(19, 4) NOT NULL DEFAULT 0,
    available NUMERIC(19, 4) NOT NULL DEFAULT 0,
    pending NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reserved NUMERIC(19, 4) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_balances_account_id ON balances(account_id);
