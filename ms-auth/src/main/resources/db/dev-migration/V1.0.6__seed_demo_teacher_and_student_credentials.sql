INSERT INTO credencial (username, password, id_usuario, activo)
SELECT
    'profesor.demo@kronofit.local',
    '$2a$10$edGU2EB0asLNvQwfN5/DHuQcaneM1MSVhL/e.f3SwXpKtbbzJtKsa',
    900002,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM credencial
    WHERE username = 'profesor.demo@kronofit.local' OR id_usuario = 900002
);

INSERT INTO credencial (username, password, id_usuario, activo)
SELECT
    'estudiante.demo@kronofit.local',
    '$2a$10$edGU2EB0asLNvQwfN5/DHuQcaneM1MSVhL/e.f3SwXpKtbbzJtKsa',
    900003,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM credencial
    WHERE username = 'estudiante.demo@kronofit.local' OR id_usuario = 900003
);
