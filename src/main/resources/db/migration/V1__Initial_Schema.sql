-- Wallets Table
CREATE TABLE wallets (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transactions Table
CREATE TABLE transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_wallet_id BIGINT,
    to_wallet_id BIGINT,
    amount DECIMAL(19,2) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    idempotency_key VARCHAR(36) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users Table
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Trigger function for wallets.updated_at
CREATE OR REPLACE FUNCTION set_wallets_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_wallets_updated_at
BEFORE UPDATE ON wallets
FOR EACH ROW
EXECUTE FUNCTION set_wallets_updated_at();

-- Indices
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_transactions_from_wallet ON transactions(from_wallet_id);
CREATE INDEX idx_transactions_to_wallet ON transactions(to_wallet_id);
CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_users_username ON users(username);
