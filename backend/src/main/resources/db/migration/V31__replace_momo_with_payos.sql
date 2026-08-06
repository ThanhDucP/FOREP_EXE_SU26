CREATE TABLE IF NOT EXISTS payos_config (
    id uuid PRIMARY KEY,
    api_endpoint text NOT NULL,
    client_id varchar(255) NOT NULL,
    api_key_encrypted text NOT NULL,
    checksum_key_encrypted text NOT NULL,
    return_url text NOT NULL,
    cancel_url text NOT NULL,
    transfer_prefix text,
    active boolean NOT NULL DEFAULT false,
    updated_by uuid REFERENCES users(id),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_payos_api_key_encrypted CHECK (api_key_encrypted LIKE 'enc:v1:%'),
    CONSTRAINT ck_payos_checksum_key_encrypted CHECK (checksum_key_encrypted LIKE 'enc:v1:%')
);

ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS payment_link_id varchar(120),
    ADD COLUMN IF NOT EXISTS reference_id uuid,
    ADD COLUMN IF NOT EXISTS user_id uuid REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS response_code varchar(40);

ALTER TABLE payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_method_valid;
ALTER TABLE payment_transactions
    ADD CONSTRAINT payment_transactions_method_valid CHECK (payment_method IN ('PAYOS', 'BANK_TRANSFER', 'MOMO'));

ALTER TABLE payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_status_valid;
ALTER TABLE payment_transactions
    ADD CONSTRAINT payment_transactions_status_valid
    CHECK (status IN ('PENDING', 'PROCESSING', 'PAID', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED', 'MANUAL_REVIEW'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_transactions_payos_link_id
    ON payment_transactions(payment_link_id) WHERE payment_link_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payment_transactions_payos_webhook
    ON payment_transactions(order_code, amount, payment_link_id) WHERE payment_method = 'PAYOS';

-- The old payment_qr_settings table remains untouched for audit/history only.
-- No runtime service reads it after this migration.
