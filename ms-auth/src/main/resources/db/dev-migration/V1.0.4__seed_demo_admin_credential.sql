INSERT INTO credencial (username, password, id_usuario, activo)
SELECT
    'admin.demo@kronofit.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    900001,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM credencial
    WHERE username = 'admin.demo@kronofit.local' OR id_usuario = 900001
);
