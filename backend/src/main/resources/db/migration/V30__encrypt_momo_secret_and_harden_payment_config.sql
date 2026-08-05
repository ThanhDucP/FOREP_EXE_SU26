-- Existing secrets were stored as plaintext and cannot be safely migrated without
-- the application encryption key. Clear them so an administrator must enter the
-- credential once after deployment; all subsequent values use enc:v1 AES-GCM.
UPDATE payment_qr_settings
SET secret_key = NULL,
    enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE payment_method = 'MOMO'
  AND secret_key IS NOT NULL;

ALTER TABLE payment_qr_settings
    ADD CONSTRAINT ck_payment_qr_settings_momo_secret_encrypted
    CHECK (secret_key IS NULL OR secret_key LIKE 'enc:v1:%');

CREATE INDEX IF NOT EXISTS idx_payment_transactions_momo_callback
    ON payment_transactions (order_code, request_id, amount)
    WHERE payment_method = 'MOMO';
