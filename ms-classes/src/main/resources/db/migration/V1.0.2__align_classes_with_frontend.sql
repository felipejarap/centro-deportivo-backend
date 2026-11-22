ALTER TABLE classe
    ADD COLUMN name VARCHAR(255) NULL,
    ADD COLUMN description TEXT NULL,
    ADD COLUMN coach_id BIGINT NULL,
    ADD COLUMN coach_name VARCHAR(255) NULL,
    ADD COLUMN intensity VARCHAR(50) NULL,
    ADD COLUMN room VARCHAR(255) NULL,
    ADD COLUMN image TEXT NULL,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE classe c
JOIN type_classes t ON t.id = c.id_type_classe
SET c.name = t.name
WHERE c.name IS NULL;
