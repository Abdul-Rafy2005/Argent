CREATE TABLE balance_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    transaction_id UUID,
    current_balance NUMERIC(19, 4) NOT NULL,
    available_balance NUMERIC(19, 4) NOT NULL,
    pending_balance NUMERIC(19, 4) NOT NULL,
    reserved_balance NUMERIC(19, 4) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    change_amount NUMERIC(19, 4) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_balance_history_account_id ON balance_history(account_id);
CREATE INDEX idx_balance_history_created_at ON balance_history(created_at);
