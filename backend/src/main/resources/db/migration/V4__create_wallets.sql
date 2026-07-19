CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    account_id UUID NOT NULL,
    label VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallets_organization_id ON wallets(organization_id);
CREATE INDEX idx_wallets_account_id ON wallets(account_id);
CREATE INDEX idx_wallets_type ON wallets(type);
CREATE INDEX idx_wallets_status ON wallets(status);
