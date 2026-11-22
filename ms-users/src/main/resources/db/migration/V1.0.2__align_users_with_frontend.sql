ALTER TABLE type_user
    ADD COLUMN description VARCHAR(255) NULL,
    ADD COLUMN permissions TEXT NULL;

ALTER TABLE `user`
    MODIFY COLUMN username VARCHAR(255) NOT NULL,
    ADD COLUMN first_name VARCHAR(100) NULL,
    ADD COLUMN last_name VARCHAR(100) NULL,
    ADD COLUMN rut VARCHAR(30) NULL,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN plan_id BIGINT NULL,
    ADD COLUMN specialty VARCHAR(255) NULL,
    ADD COLUMN bio TEXT NULL,
    ADD COLUMN emergency_contact VARCHAR(255) NULL,
    ADD COLUMN avatar TEXT NULL,
    ADD COLUMN registration_date DATE NULL;

UPDATE `user`
SET first_name = paternal_surname,
    last_name = maternal_surname,
    registration_date = CURRENT_DATE
WHERE first_name IS NULL;
