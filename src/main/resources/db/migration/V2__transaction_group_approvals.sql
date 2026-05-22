-- Group consensus when AI rejects a transaction (optional if Flyway is enabled later)
CREATE TABLE transaction_group_approvals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (transaction_id, user_id)
);

CREATE INDEX idx_tx_group_approvals_transaction ON transaction_group_approvals(transaction_id);
CREATE INDEX idx_tx_group_approvals_user ON transaction_group_approvals(user_id);
