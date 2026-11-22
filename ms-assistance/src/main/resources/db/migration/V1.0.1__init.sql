CREATE TABLE assistance (
    id_assistance BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_user    BIGINT      NOT NULL,
    id_classe     BIGINT      NOT NULL,
    arrival_time  DATETIME  NOT NULL,
    assist       BOOLEAN     NOT NULL DEFAULT FALSE
);
