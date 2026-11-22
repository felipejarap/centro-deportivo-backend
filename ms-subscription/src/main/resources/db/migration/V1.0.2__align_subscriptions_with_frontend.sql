ALTER TABLE subscription
    ADD COLUMN start_date DATETIME NULL,
    ADD COLUMN auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payment_method VARCHAR(100) NULL;

UPDATE subscription
SET start_date = CURRENT_TIMESTAMP
WHERE start_date IS NULL;
