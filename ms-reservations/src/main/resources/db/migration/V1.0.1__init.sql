CREATE TABLE reservation (
    id_reservation     BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_user     BIGINT NOT NULL,
    id_classe       BIGINT NOT NULL,
    id_coach  BIGINT NOT NULL,
    reservation_date  DATETIME  NOT NULL,
    reservation_status VARCHAR(50)  NOT NULL
);
