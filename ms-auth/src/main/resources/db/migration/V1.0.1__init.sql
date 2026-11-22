CREATE TABLE credencial (
    id_credencial BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    id_usuario    BIGINT       NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
);
