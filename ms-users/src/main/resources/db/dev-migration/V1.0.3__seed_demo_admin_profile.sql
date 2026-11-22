INSERT INTO `user` (
    id_user,
    username,
    paternal_surname,
    maternal_surname,
    email,
    phone,
    id_type_user,
    first_name,
    last_name,
    active,
    registration_date
)
SELECT
    900001,
    'admin.demo@kronofit.local',
    'Kronofit',
    'Demo',
    'admin.demo@kronofit.local',
    NULL,
    (SELECT id FROM type_user WHERE name = 'Admin' LIMIT 1),
    'Admin',
    'Demo',
    TRUE,
    CURRENT_DATE
WHERE NOT EXISTS (
    SELECT 1
    FROM `user`
    WHERE id_user = 900001 OR username = 'admin.demo@kronofit.local'
);
