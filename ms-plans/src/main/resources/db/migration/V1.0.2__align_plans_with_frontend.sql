ALTER TABLE plans
    ADD COLUMN period VARCHAR(50) NULL,
    ADD COLUMN classes_per_month VARCHAR(255) NULL,
    ADD COLUMN benefits TEXT NULL,
    ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE plans
SET period = 'Mensual',
    classes_per_month = 'Clases según disponibilidad'
WHERE period IS NULL;
